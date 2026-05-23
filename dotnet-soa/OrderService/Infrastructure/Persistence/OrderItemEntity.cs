namespace OrderService.Infrastructure.Persistence;

/// <summary>
/// Order item persistence entity mapped to the order_items database table.
/// </summary>
public class OrderItemEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid OrderId { get; set; }
    public Guid ProductId { get; set; }
    public string ProductName { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public decimal UnitPrice { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
