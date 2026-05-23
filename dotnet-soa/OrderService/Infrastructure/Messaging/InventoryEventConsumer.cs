using System.Text.Json;
using Confluent.Kafka;
using OrderService.Domain.Interfaces;

namespace OrderService.Infrastructure.Messaging;

/// <summary>
/// Kafka consumer for inventory events - implemented as a BackgroundService.
/// 
/// SOA Principle: Asynchronous Communication - this consumer listens for
/// events from the Inventory Service via the ESB (Kafka), enabling
/// loose coupling between services.
/// 
/// Design Pattern: Saga Participant - reacts to inventory events to
/// advance or compensate the order creation saga.
/// 
/// Subscribes to:
/// - stock.reserved → Confirms the order (Saga happy path)
/// - stock.insufficient → Cancels the order (Saga compensation)
/// </summary>
public class InventoryEventConsumer : BackgroundService
{
    private readonly IConsumer<string, string> _consumer;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<InventoryEventConsumer> _logger;

    public InventoryEventConsumer(
        IConfiguration config,
        IServiceScopeFactory scopeFactory,
        ILogger<InventoryEventConsumer> logger)
    {
        var consumerConfig = new ConsumerConfig
        {
            BootstrapServers = config["Kafka:BootstrapServers"] ?? "localhost:9095",
            GroupId = "order-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };
        _consumer = new ConsumerBuilder<string, string>(consumerConfig).Build();
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _consumer.Subscribe(new[] { "stock.reserved", "stock.insufficient" });
        _logger.LogInformation("InventoryEventConsumer started. Listening for stock events...");

        await Task.Run(() =>
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = _consumer.Consume(stoppingToken);
                    _logger.LogInformation("Received event on topic {Topic}: {Value}", result.Topic, result.Message.Value);

                    using var scope = _scopeFactory.CreateScope();
                    var orderService = scope.ServiceProvider.GetRequiredService<IOrderService>();

                    switch (result.Topic)
                    {
                        case "stock.reserved":
                            HandleStockReserved(result.Message.Value, orderService).GetAwaiter().GetResult();
                            break;

                        case "stock.insufficient":
                            HandleStockInsufficient(result.Message.Value, orderService).GetAwaiter().GetResult();
                            break;
                    }
                }
                catch (ConsumeException ex)
                {
                    _logger.LogError(ex, "Error consuming Kafka message");
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Unexpected error in InventoryEventConsumer");
                }
            }
        }, stoppingToken);

        _consumer.Close();
    }

    private async Task HandleStockReserved(string messageValue, IOrderService orderService)
    {
        var evt = JsonSerializer.Deserialize<StockReservedEvent>(messageValue, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });

        if (evt is not null)
        {
            _logger.LogInformation("Processing stock.reserved for order {OrderId}", evt.OrderId);
            await orderService.HandleStockReservedAsync(evt.OrderId);
        }
    }

    private async Task HandleStockInsufficient(string messageValue, IOrderService orderService)
    {
        var evt = JsonSerializer.Deserialize<StockInsufficientEvent>(messageValue, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });

        if (evt is not null)
        {
            _logger.LogInformation("Processing stock.insufficient for order {OrderId}", evt.OrderId);
            await orderService.HandleInsufficientStockAsync(evt.OrderId, evt.Reason);
        }
    }

    public override void Dispose()
    {
        _consumer?.Dispose();
        base.Dispose();
    }
}
