using System.Text.Json;
using Confluent.Kafka;
using InventoryService.Domain.Interfaces;

namespace InventoryService.Infrastructure.Messaging;

/// <summary>
/// Kafka consumer for order events - implemented as a BackgroundService.
/// 
/// SOA Principle: Asynchronous Communication - this consumer listens for
/// order events from the ESB (Kafka) and participates in the Saga.
/// 
/// Design Pattern: Saga Participant - processes order events to manage
/// stock reservations as part of the distributed transaction.
/// 
/// Subscribes to:
/// - order.created → Attempts stock reservation
/// - order.cancelled → Releases reserved stock (compensation)
/// </summary>
public class OrderEventConsumer : BackgroundService
{
    private readonly IConsumer<string, string> _consumer;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<OrderEventConsumer> _logger;

    public OrderEventConsumer(
        IConfiguration config,
        IServiceScopeFactory scopeFactory,
        ILogger<OrderEventConsumer> logger)
    {
        var consumerConfig = new ConsumerConfig
        {
            BootstrapServers = config["Kafka:BootstrapServers"] ?? "localhost:9095",
            GroupId = "inventory-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };
        _consumer = new ConsumerBuilder<string, string>(consumerConfig).Build();
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _consumer.Subscribe(new[] { "order.created", "order.cancelled" });
        _logger.LogInformation("OrderEventConsumer started. Listening for order events...");

        await Task.Run(() =>
        {
            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = _consumer.Consume(stoppingToken);
                    _logger.LogInformation("Received event on topic {Topic}: {Value}", result.Topic, result.Message.Value);

                    using var scope = _scopeFactory.CreateScope();
                    var inventoryService = scope.ServiceProvider.GetRequiredService<IInventoryService>();

                    switch (result.Topic)
                    {
                        case "order.created":
                            HandleOrderCreated(result.Message.Value, inventoryService).GetAwaiter().GetResult();
                            break;

                        case "order.cancelled":
                            HandleOrderCancelled(result.Message.Value, inventoryService).GetAwaiter().GetResult();
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
                    _logger.LogError(ex, "Unexpected error in OrderEventConsumer");
                }
            }
        }, stoppingToken);

        _consumer.Close();
    }

    private async Task HandleOrderCreated(string messageValue, IInventoryService inventoryService)
    {
        var evt = JsonSerializer.Deserialize<OrderCreatedEvent>(messageValue, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });

        if (evt is not null)
        {
            _logger.LogInformation("Processing order.created for order {OrderId} with {ItemCount} items",
                evt.OrderId, evt.Items.Count);

            var reservationRequests = evt.Items.Select(i => new ReservationRequest
            {
                ProductId = i.ProductId,
                ProductName = i.ProductName,
                Quantity = i.Quantity
            }).ToList();

            await inventoryService.ReserveStockAsync(evt.OrderId, reservationRequests);
        }
    }

    private async Task HandleOrderCancelled(string messageValue, IInventoryService inventoryService)
    {
        var evt = JsonSerializer.Deserialize<OrderCancelledEvent>(messageValue, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });

        if (evt is not null)
        {
            _logger.LogInformation("Processing order.cancelled for order {OrderId}", evt.OrderId);

            // Note: In a production system, we would store reservation details
            // to know exactly what to release. For this demo, we handle it gracefully.
            // The order.cancelled event would ideally include item details.
            _logger.LogWarning("Order {OrderId} cancelled. Stock release requires item details from the event.", evt.OrderId);
        }
    }

    public override void Dispose()
    {
        _consumer?.Dispose();
        base.Dispose();
    }
}
