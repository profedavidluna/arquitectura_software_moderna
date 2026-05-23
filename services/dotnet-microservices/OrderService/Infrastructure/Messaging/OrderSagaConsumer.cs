namespace OrderService.Infrastructure.Messaging;

using System.Text.Json;
using Confluent.Kafka;
using Microsoft.Extensions.DependencyInjection;
using OrderService.Domain.Interfaces;
using OrderService.Domain.Models;

public class OrderSagaConsumer : BackgroundService
{
    private readonly ILogger<OrderSagaConsumer> _logger;
    private readonly IConfiguration _configuration;
    private readonly IServiceProvider _serviceProvider;

    public OrderSagaConsumer(ILogger<OrderSagaConsumer> logger, IConfiguration configuration, IServiceProvider serviceProvider)
    {
        _logger = logger;
        _configuration = configuration;
        _serviceProvider = serviceProvider;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var bootstrapServers = _configuration["Kafka:BootstrapServers"];
        if (string.IsNullOrEmpty(bootstrapServers))
        {
            _logger.LogWarning("Kafka not configured. OrderSagaConsumer disabled.");
            return;
        }

        await Task.Delay(5000, stoppingToken);

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = _configuration["Kafka:GroupId"] ?? "order-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        try
        {
            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            consumer.Subscribe(new[] { "inventory-events", "payment-events" });

            _logger.LogInformation("OrderSagaConsumer started, listening on inventory-events, payment-events");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromSeconds(1));
                    if (result != null)
                    {
                        await ProcessSagaEventAsync(result.Message.Value);
                    }
                }
                catch (ConsumeException ex)
                {
                    _logger.LogError(ex, "Error consuming saga message");
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "OrderSagaConsumer failed");
        }
    }

    private async Task ProcessSagaEventAsync(string messageJson)
    {
        try
        {
            var message = JsonSerializer.Deserialize<JsonElement>(messageJson);
            var eventType = message.GetProperty("EventType").GetString();
            var orderId = Guid.Parse(message.GetProperty("OrderId").GetString()!);

            using var scope = _serviceProvider.CreateScope();
            var orderService = scope.ServiceProvider.GetRequiredService<IOrderService>();

            switch (eventType)
            {
                case "InventoryReserved":
                    _logger.LogInformation("Inventory reserved for order {OrderId}, advancing saga", orderId);
                    await orderService.AdvanceSagaAsync(orderId, SagaStates.InventoryReserved);
                    break;

                case "InventoryReservationFailed":
                    _logger.LogWarning("Inventory reservation failed for order {OrderId}, compensating", orderId);
                    await orderService.CompensateSagaAsync(orderId, "Inventory reservation failed");
                    break;

                case "PaymentProcessed":
                    _logger.LogInformation("Payment processed for order {OrderId}, advancing saga", orderId);
                    await orderService.AdvanceSagaAsync(orderId, SagaStates.PaymentProcessed);
                    break;

                case "PaymentFailed":
                    _logger.LogWarning("Payment failed for order {OrderId}, compensating", orderId);
                    await orderService.CompensateSagaAsync(orderId, "Payment processing failed");
                    break;

                default:
                    _logger.LogDebug("Unhandled saga event: {EventType}", eventType);
                    break;
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing saga event");
        }
    }
}
