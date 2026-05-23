namespace OrderService.Domain.Models;

public record Order(
    Guid Id,
    Guid UserId,
    string Status,
    decimal TotalAmount,
    string? ShippingAddress,
    string SagaState,
    List<OrderItem> Items,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

public record OrderItem(
    Guid Id,
    Guid OrderId,
    Guid ProductId,
    string ProductName,
    decimal UnitPrice,
    int Quantity
);

public record CreateOrderRequest(
    Guid UserId,
    string? ShippingAddress,
    List<OrderItemRequest> Items
);

public record OrderItemRequest(
    Guid ProductId,
    string ProductName,
    decimal UnitPrice,
    int Quantity
);

// Saga states
public static class SagaStates
{
    public const string Initiated = "initiated";
    public const string InventoryReserved = "inventory_reserved";
    public const string PaymentProcessed = "payment_processed";
    public const string Confirmed = "confirmed";
    public const string Failed = "failed";
    public const string Compensating = "compensating";
    public const string Cancelled = "cancelled";
}

public static class OrderStatuses
{
    public const string Pending = "pending";
    public const string Confirmed = "confirmed";
    public const string Shipped = "shipped";
    public const string Delivered = "delivered";
    public const string Cancelled = "cancelled";
}
