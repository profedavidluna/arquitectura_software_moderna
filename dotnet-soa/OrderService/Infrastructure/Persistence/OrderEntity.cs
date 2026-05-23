namespace OrderService.Infrastructure.Persistence;

/// <summary>
/// Order persistence entity mapped to the orders database table.
/// </summary>
public class OrderEntity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public string CustomerName { get; set; } = string.Empty;
    public string CustomerEmail { get; set; } = string.Empty;
    public string Status { get; set; } = "PENDING";
    public decimal TotalAmount { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    public List<OrderItemEntity> Items { get; set; } = new();
}
