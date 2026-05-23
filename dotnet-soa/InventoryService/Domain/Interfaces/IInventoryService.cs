using InventoryService.Domain.Models;

namespace InventoryService.Domain.Interfaces;

/// <summary>
/// Inventory service interface following ISP.
/// 
/// SOA Principle: Service Contract - defines inventory management
/// operations and Saga participation methods.
/// </summary>
public interface IInventoryService
{
    /// <summary>Creates or updates inventory for a product.</summary>
    Task<InventoryItem> CreateOrUpdateInventoryAsync(Guid productId, string productName, int quantity);

    /// <summary>Retrieves all inventory items.</summary>
    Task<IEnumerable<InventoryItem>> GetAllInventoryAsync();

    /// <summary>Retrieves inventory by product ID.</summary>
    Task<InventoryItem?> GetByProductIdAsync(Guid productId);

    /// <summary>
    /// Attempts to reserve stock for an order (Saga participant action).
    /// Publishes stock.reserved or stock.insufficient event.
    /// </summary>
    Task ReserveStockAsync(Guid orderId, List<ReservationRequest> items);

    /// <summary>
    /// Releases previously reserved stock (Saga compensation action).
    /// Called when an order is cancelled after stock was reserved.
    /// </summary>
    Task ReleaseStockAsync(Guid orderId, List<ReservationRequest> items);
}

/// <summary>Stock reservation request for a single product.</summary>
public record ReservationRequest
{
    public Guid ProductId { get; init; }
    public string ProductName { get; init; } = string.Empty;
    public int Quantity { get; init; }
}
