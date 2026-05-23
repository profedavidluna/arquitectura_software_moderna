namespace InventoryService.Infrastructure.Messaging;

/// <summary>
/// Domain events for the Inventory Service.
/// 
/// Published events:
/// - stock.reserved: Stock successfully reserved for an order
/// - stock.insufficient: Not enough stock available for an order
/// - stock.released: Reserved stock released after order cancellation
/// 
/// Consumed events:
/// - order.created: New order requiring stock reservation
/// - order.cancelled: Order cancelled, release reserved stock
/// </summary>

/// <summary>Published when stock is successfully reserved</summary>
public record StockReservedEvent
{
    public Guid OrderId { get; init; }
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}

/// <summary>Published when stock is insufficient for an order</summary>
public record StockInsufficientEvent
{
    public Guid OrderId { get; init; }
    public string Reason { get; init; } = string.Empty;
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}

/// <summary>Published when reserved stock is released</summary>
public record StockReleasedEvent
{
    public Guid OrderId { get; init; }
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}

/// <summary>Consumed from Order Service when a new order is created</summary>
public record OrderCreatedEvent
{
    public Guid OrderId { get; init; }
    public string CustomerName { get; init; } = string.Empty;
    public string CustomerEmail { get; init; } = string.Empty;
    public List<OrderItemEvent> Items { get; init; } = new();
    public decimal TotalAmount { get; init; }
    public DateTime Timestamp { get; init; }
}

/// <summary>Order item within an order event</summary>
public record OrderItemEvent
{
    public Guid ProductId { get; init; }
    public string ProductName { get; init; } = string.Empty;
    public int Quantity { get; init; }
    public decimal UnitPrice { get; init; }
}

/// <summary>Consumed from Order Service when an order is cancelled</summary>
public record OrderCancelledEvent
{
    public Guid OrderId { get; init; }
    public string Reason { get; init; } = string.Empty;
    public DateTime Timestamp { get; init; }
}
