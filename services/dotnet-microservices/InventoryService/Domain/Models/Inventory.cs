namespace InventoryService.Domain.Models;

public record InventoryItem(
    Guid Id,
    Guid ProductId,
    string ProductName,
    int Quantity,
    int ReservedQuantity,
    int MinStockLevel,
    DateTime CreatedAt,
    DateTime UpdatedAt
)
{
    public int AvailableQuantity => Quantity - ReservedQuantity;
    public bool IsLowStock => AvailableQuantity <= MinStockLevel;
}

public record Reservation(
    Guid Id,
    Guid OrderId,
    Guid ProductId,
    int Quantity,
    string Status,
    DateTime? ExpiresAt,
    DateTime CreatedAt
);

public record CreateInventoryItemRequest(Guid ProductId, string ProductName, int Quantity, int MinStockLevel = 10);
public record UpdateStockRequest(int QuantityChange);
public record ReserveStockRequest(Guid OrderId, List<ReserveItemRequest> Items);
public record ReserveItemRequest(Guid ProductId, int Quantity);
