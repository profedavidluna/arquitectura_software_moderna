using InventoryService.Domain.Interfaces;
using InventoryService.Domain.Models;
using InventoryService.Infrastructure.Messaging;
using InventoryService.Infrastructure.Persistence;

namespace InventoryService.Application;

/// <summary>
/// Inventory service implementation - Saga Participant.
/// 
/// Design Pattern: Saga Participant - this service participates in the
/// order creation saga by attempting stock reservation and publishing
/// the result back to the ESB for the orchestrator (Order Service).
/// 
/// Saga Participation:
/// 1. Receives "order.created" → attempts stock reservation
/// 2a. Success → publishes "stock.reserved"
/// 2b. Failure → publishes "stock.insufficient"
/// 3. Receives "order.cancelled" → releases reserved stock (compensation)
/// </summary>
public class InventoryServiceImpl : IInventoryService
{
    private readonly InventoryRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<InventoryServiceImpl> _logger;

    public InventoryServiceImpl(
        InventoryRepository repository,
        KafkaProducer kafkaProducer,
        ILogger<InventoryServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    /// <inheritdoc />
    public async Task<InventoryItem> CreateOrUpdateInventoryAsync(Guid productId, string productName, int quantity)
    {
        var existing = await _repository.GetByProductIdAsync(productId);

        if (existing is not null)
        {
            return await _repository.UpdateQuantityAsync(productId, existing.Quantity + quantity);
        }

        var item = new InventoryItem
        {
            ProductId = productId,
            ProductName = productName,
            Quantity = quantity,
            ReservedQuantity = 0
        };

        return await _repository.CreateAsync(item);
    }

    /// <inheritdoc />
    public async Task<IEnumerable<InventoryItem>> GetAllInventoryAsync()
    {
        return await _repository.GetAllAsync();
    }

    /// <inheritdoc />
    public async Task<InventoryItem?> GetByProductIdAsync(Guid productId)
    {
        return await _repository.GetByProductIdAsync(productId);
    }

    /// <inheritdoc />
    public async Task ReserveStockAsync(Guid orderId, List<ReservationRequest> items)
    {
        _logger.LogInformation("Attempting stock reservation for order {OrderId}", orderId);

        // Check availability for all items
        foreach (var item in items)
        {
            var inventory = await _repository.GetByProductIdAsync(item.ProductId);

            if (inventory is null || inventory.AvailableQuantity < item.Quantity)
            {
                var reason = inventory is null
                    ? $"Product {item.ProductName} not found in inventory"
                    : $"Insufficient stock for {item.ProductName}. Available: {inventory.AvailableQuantity}, Requested: {item.Quantity}";

                _logger.LogWarning("Stock reservation failed for order {OrderId}: {Reason}", orderId, reason);

                // Publish stock.insufficient event
                await PublishStockInsufficientAsync(orderId, reason);
                return;
            }
        }

        // Reserve stock for all items
        foreach (var item in items)
        {
            await _repository.ReserveStockAsync(item.ProductId, item.Quantity);
        }

        _logger.LogInformation("Stock reserved successfully for order {OrderId}", orderId);

        // Publish stock.reserved event
        try
        {
            await _kafkaProducer.PublishAsync("stock.reserved", orderId.ToString(), new StockReservedEvent
            {
                OrderId = orderId,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Published stock.reserved event for order {OrderId}", orderId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish stock.reserved event for order {OrderId}", orderId);
        }
    }

    /// <inheritdoc />
    public async Task ReleaseStockAsync(Guid orderId, List<ReservationRequest> items)
    {
        _logger.LogInformation("Releasing reserved stock for cancelled order {OrderId}", orderId);

        foreach (var item in items)
        {
            await _repository.ReleaseStockAsync(item.ProductId, item.Quantity);
        }

        // Publish stock.released event
        try
        {
            await _kafkaProducer.PublishAsync("stock.released", orderId.ToString(), new StockReleasedEvent
            {
                OrderId = orderId,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Published stock.released event for order {OrderId}", orderId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish stock.released event for order {OrderId}", orderId);
        }
    }

    private async Task PublishStockInsufficientAsync(Guid orderId, string reason)
    {
        try
        {
            await _kafkaProducer.PublishAsync("stock.insufficient", orderId.ToString(), new StockInsufficientEvent
            {
                OrderId = orderId,
                Reason = reason,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Published stock.insufficient event for order {OrderId}", orderId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish stock.insufficient event for order {OrderId}", orderId);
        }
    }
}
