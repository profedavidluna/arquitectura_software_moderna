namespace OrderService.Domain.Models;

/// <summary>
/// Order domain model representing the aggregate root for order management.
/// 
/// SOA Principle: Each service owns its domain model independently.
/// The Order aggregate encapsulates the order lifecycle managed by the Saga.
/// </summary>
public record Order
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public string CustomerName { get; init; } = string.Empty;
    public string CustomerEmail { get; init; } = string.Empty;
    public OrderStatus Status { get; init; } = OrderStatus.PENDING;
    public decimal TotalAmount { get; init; }
    public List<OrderItem> Items { get; init; } = new();
    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; init; } = DateTime.UtcNow;
}
