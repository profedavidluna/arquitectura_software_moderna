namespace InventoryService.Infrastructure.Messaging;

using System.Text.Json;
using Confluent.Kafka;
using InventoryService.Domain.Interfaces;
using InventoryService.Domain.Models;

public class InventoryCommandConsumer : BackgroundService
{
    private readonly ILogger<InventoryCommandConsumer> _logger;
    private readonly IConfiguration _configuration;
    private readonly IServiceProvider _serviceProvider;

    public InventoryCommandConsumer(ILogger<InventoryCommandConsumer> logger, IConfiguration configuration, IServiceProvider serviceProvider)
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
            _logger.LogWarning("Kafka not configured. InventoryCommandConsumer disabled.");
            return;
        }

        await Task.Delay(5000, stoppingToken);

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = _configuration["Kafka:GroupId"] ?? "inventory-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        try
        {
            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            consumer.Subscribe(new[] { "order-events", "inventory-commands" });

            _logger.LogInformation("InventoryCommandConsumer started");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromSeconds(1));
                    if (result != null) await ProcessCommandAsync(result.Message.Value);
                }
                catch (ConsumeException ex) { _logger.LogError(ex, "Error consuming"); }
            }
        }
        catch (Exception ex) { _logger.LogError(ex, "InventoryCommandConsumer failed"); }
    }

    private async Task ProcessCommandAsync(string messageJson)
    {
        try
        {
            var message = JsonSerializer.Deserialize<JsonElement>(messageJson);
            var eventType = message.GetProperty("EventType").GetString();

            using var scope = _serviceProvider.CreateScope();
            var inventoryService = scope.ServiceProvider.GetRequiredService<IInventoryService>();

            switch (eventType)
            {
                case "OrderCreated":
                    var orderId = Guid.Parse(message.GetProperty("OrderId").GetString()!);
                    var items = message.GetProperty("Items").EnumerateArray()
                        .Select(i => new ReserveItemRequest(
                            Guid.Parse(i.GetProperty("ProductId").GetString()!),
                            i.GetProperty("Quantity").GetInt32()
                        )).ToList();
                    await inventoryService.ReserveStockAsync(new ReserveStockRequest(orderId, items));
                    break;

                case "ReleaseReservation":
                    var releaseOrderId = Guid.Parse(message.GetProperty("OrderId").GetString()!);
                    await inventoryService.ReleaseReservationAsync(releaseOrderId);
                    break;

                case "OrderConfirmed":
                    var confirmOrderId = Guid.Parse(message.GetProperty("OrderId").GetString()!);
                    await inventoryService.ConfirmReservationAsync(confirmOrderId);
                    break;
            }
        }
        catch (Exception ex) { _logger.LogError(ex, "Error processing inventory command"); }
    }
}
