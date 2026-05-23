namespace InventoryService.Application;

using InventoryService.Domain.Interfaces;
using InventoryService.Domain.Models;
using InventoryService.Infrastructure.Messaging;

public class InventoryServiceImpl : IInventoryService
{
    private readonly IInventoryRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<InventoryServiceImpl> _logger;

    public InventoryServiceImpl(IInventoryRepository repository, KafkaProducer kafkaProducer, ILogger<InventoryServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    public async Task<InventoryItem?> GetByProductIdAsync(Guid productId) => await _repository.GetByProductIdAsync(productId);

    public async Task<IEnumerable<InventoryItem>> GetAllAsync() => await _repository.GetAllAsync();

    public async Task<InventoryItem> CreateItemAsync(CreateInventoryItemRequest request)
    {
        var existing = await _repository.GetByProductIdAsync(request.ProductId);
        if (existing != null)
            throw new InvalidOperationException($"Inventory item for product {request.ProductId} already exists");

        var item = new InventoryItem(
            Id: Guid.NewGuid(),
            ProductId: request.ProductId,
            ProductName: request.ProductName,
            Quantity: request.Quantity,
            ReservedQuantity: 0,
            MinStockLevel: request.MinStockLevel,
            CreatedAt: DateTime.UtcNow,
            UpdatedAt: DateTime.UtcNow
        );

        return await _repository.CreateAsync(item);
    }

    public async Task<InventoryItem> UpdateStockAsync(Guid productId, UpdateStockRequest request)
    {
        var item = await _repository.GetByProductIdAsync(productId)
            ?? throw new KeyNotFoundException($"Inventory item for product {productId} not found");

        var newQuantity = item.Quantity + request.QuantityChange;
        if (newQuantity < 0)
            throw new InvalidOperationException("Insufficient stock");

        var updated = item with { Quantity = newQuantity, UpdatedAt = DateTime.UtcNow };
        var result = await _repository.UpdateAsync(updated);

        if (result.IsLowStock)
        {
            await _kafkaProducer.PublishAsync("inventory-events", new
            {
                EventType = "LowStock",
                ProductId = productId,
                ProductName = result.ProductName,
                AvailableQuantity = result.AvailableQuantity,
                Timestamp = DateTime.UtcNow
            });
        }

        return result;
    }

    public async Task<bool> ReserveStockAsync(ReserveStockRequest request)
    {
        _logger.LogInformation("Reserving stock for order {OrderId}", request.OrderId);

        foreach (var item in request.Items)
        {
            var inventory = await _repository.GetByProductIdAsync(item.ProductId);
            if (inventory == null || inventory.AvailableQuantity < item.Quantity)
            {
                _logger.LogWarning("Insufficient stock for product {ProductId}", item.ProductId);

                await _kafkaProducer.PublishAsync("inventory-events", new
                {
                    EventType = "InventoryReservationFailed",
                    OrderId = request.OrderId,
                    ProductId = item.ProductId,
                    Reason = "Insufficient stock",
                    Timestamp = DateTime.UtcNow
                });
                return false;
            }
        }

        // Reserve all items
        foreach (var item in request.Items)
        {
            var inventory = (await _repository.GetByProductIdAsync(item.ProductId))!;
            var updated = inventory with
            {
                ReservedQuantity = inventory.ReservedQuantity + item.Quantity,
                UpdatedAt = DateTime.UtcNow
            };
            await _repository.UpdateAsync(updated);

            var reservation = new Reservation(
                Guid.NewGuid(), request.OrderId, item.ProductId,
                item.Quantity, "reserved", DateTime.UtcNow.AddMinutes(30), DateTime.UtcNow
            );
            await _repository.CreateReservationAsync(reservation);
        }

        await _kafkaProducer.PublishAsync("inventory-events", new
        {
            EventType = "InventoryReserved",
            OrderId = request.OrderId,
            Items = request.Items,
            Timestamp = DateTime.UtcNow
        });

        return true;
    }

    public async Task<bool> ReleaseReservationAsync(Guid orderId)
    {
        var reservations = await _repository.GetReservationsByOrderIdAsync(orderId);
        foreach (var reservation in reservations.Where(r => r.Status == "reserved"))
        {
            var inventory = await _repository.GetByProductIdAsync(reservation.ProductId);
            if (inventory != null)
            {
                var updated = inventory with
                {
                    ReservedQuantity = Math.Max(0, inventory.ReservedQuantity - reservation.Quantity),
                    UpdatedAt = DateTime.UtcNow
                };
                await _repository.UpdateAsync(updated);
            }
        }

        await _repository.UpdateReservationStatusAsync(orderId, "released");
        _logger.LogInformation("Released reservations for order {OrderId}", orderId);
        return true;
    }

    public async Task<bool> ConfirmReservationAsync(Guid orderId)
    {
        var reservations = await _repository.GetReservationsByOrderIdAsync(orderId);
        foreach (var reservation in reservations.Where(r => r.Status == "reserved"))
        {
            var inventory = await _repository.GetByProductIdAsync(reservation.ProductId);
            if (inventory != null)
            {
                var updated = inventory with
                {
                    Quantity = inventory.Quantity - reservation.Quantity,
                    ReservedQuantity = Math.Max(0, inventory.ReservedQuantity - reservation.Quantity),
                    UpdatedAt = DateTime.UtcNow
                };
                await _repository.UpdateAsync(updated);
            }
        }

        await _repository.UpdateReservationStatusAsync(orderId, "confirmed");
        _logger.LogInformation("Confirmed reservations for order {OrderId}", orderId);
        return true;
    }
}
