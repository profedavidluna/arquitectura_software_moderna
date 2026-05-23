namespace CartService.Domain.Models;

public record Cart(
    Guid Id,
    Guid UserId,
    string Status,
    string? CouponCode,
    decimal DiscountPercent,
    List<CartItem> Items,
    DateTime CreatedAt,
    DateTime UpdatedAt
)
{
    public decimal Subtotal => Items.Sum(i => i.UnitPrice * i.Quantity);
    public decimal DiscountAmount => Subtotal * (DiscountPercent / 100m);
    public decimal Total => Subtotal - DiscountAmount;
}

public record CartItem(
    Guid Id,
    Guid CartId,
    Guid ProductId,
    string ProductName,
    decimal UnitPrice,
    int Quantity,
    DateTime CreatedAt
);

public record AddItemRequest(Guid ProductId, int Quantity);
public record UpdateItemQuantityRequest(int Quantity);
public record ApplyCouponRequest(string CouponCode);
