namespace OrderService.Infrastructure.Persistence.Entities;

public class OrderEntity
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string Status { get; set; } = "pending";
    public decimal TotalAmount { get; set; }
    public string? ShippingAddress { get; set; }
    public string SagaState { get; set; } = "initiated";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    public List<OrderItemEntity> Items { get; set; } = new();
}

public class OrderItemEntity
{
    public Guid Id { get; set; }
    public Guid OrderId { get; set; }
    public Guid ProductId { get; set; }
    public string ProductName { get; set; } = string.Empty;
    public decimal UnitPrice { get; set; }
    public int Quantity { get; set; }
    public OrderEntity Order { get; set; } = null!;
}
