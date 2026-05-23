namespace OrderService.Infrastructure.Messaging;

/// <summary>
/// Domain events for the Order Service Saga orchestration.
/// 
/// These events represent the communication protocol between
/// Order Service and Inventory Service in the distributed transaction.
/// </summary>

/// <summary>Published when a new order is created (Saga Step 1)</summary>
public record OrderCreatedEvent
{
    public Guid OrderId { get; init; }
    public string CustomerName { get; init; } = string.Empty;
    public string CustomerEmail { get; init; } = string.Empty;
    public List<OrderItemEvent> Items { get; init; } = new();
    public decimal TotalAmount { get; init; }
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}

/// <summary>Order item within an order event</summary>
public record OrderItemEvent
{
    public Guid ProductId { get; init; }
    public string ProductName { get; init; } = string.Empty;
    public int Quantity { get; init; }
    public decimal UnitPrice { get; init; }
}

/// <summary>Published when order is confirmed after stock reservation (Saga completion)</summary>
public record OrderConfirmedEvent
{
    public Guid OrderId { get; init; }
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}

/// <summary>Published when order is cancelled due to insufficient stock (Saga compensation)</summary>
public record OrderCancelledEvent
{
    public Guid OrderId { get; init; }
    public string Reason { get; init; } = string.Empty;
    public DateTime Timestamp { get; init; } = DateTime.UtcNow;
}

/// <summary>Consumed from Inventory Service when stock is reserved</summary>
public record StockReservedEvent
{
    public Guid OrderId { get; init; }
    public DateTime Timestamp { get; init; }
}

/// <summary>Consumed from Inventory Service when stock is insufficient</summary>
public record StockInsufficientEvent
{
    public Guid OrderId { get; init; }
    public string Reason { get; init; } = string.Empty;
    public DateTime Timestamp { get; init; }
}
