namespace InventoryService.Domain.Models;

/// <summary>
/// Inventory item domain model representing stock levels for a product.
/// 
/// SOA Principle: Each service owns its domain model. The Inventory Service
/// tracks stock independently from the Product Service's catalog data.
/// </summary>
public record InventoryItem
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public Guid ProductId { get; init; }
    public string ProductName { get; init; } = string.Empty;
    public int Quantity { get; init; }
    public int ReservedQuantity { get; init; }

    /// <summary>Available quantity = total quantity - reserved quantity</summary>
    public int AvailableQuantity => Quantity - ReservedQuantity;

    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; init; } = DateTime.UtcNow;
}
