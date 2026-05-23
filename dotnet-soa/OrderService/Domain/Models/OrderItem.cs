namespace OrderService.Domain.Models;

/// <summary>
/// Order item domain model representing a line item in an order.
/// </summary>
public record OrderItem
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public Guid OrderId { get; init; }
    public Guid ProductId { get; init; }
    public string ProductName { get; init; } = string.Empty;
    public int Quantity { get; init; }
    public decimal UnitPrice { get; init; }
    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;
}
