using System.ComponentModel.DataAnnotations;

namespace OrderService.Infrastructure.Web.Dto;

/// <summary>
/// DTO for order creation requests.
/// </summary>
public record CreateOrderRequest
{
    [Required]
    [StringLength(255)]
    public string CustomerName { get; init; } = string.Empty;

    [Required]
    [EmailAddress]
    public string CustomerEmail { get; init; } = string.Empty;

    [Required]
    [MinLength(1, ErrorMessage = "At least one item is required")]
    public List<OrderItemRequest> Items { get; init; } = new();
}

public record OrderItemRequest
{
    [Required]
    public Guid ProductId { get; init; }

    [Required]
    public string ProductName { get; init; } = string.Empty;

    [Required]
    [Range(1, int.MaxValue, ErrorMessage = "Quantity must be at least 1")]
    public int Quantity { get; init; }

    [Required]
    [Range(0.01, double.MaxValue, ErrorMessage = "Unit price must be greater than zero")]
    public decimal UnitPrice { get; init; }
}
