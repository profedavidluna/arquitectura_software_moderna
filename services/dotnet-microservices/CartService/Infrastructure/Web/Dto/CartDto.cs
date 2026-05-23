namespace CartService.Infrastructure.Web.Dto;

public record CartDto(
    Guid Id, Guid UserId, string Status, string? CouponCode, decimal DiscountPercent,
    List<CartItemDto> Items, decimal Subtotal, decimal DiscountAmount, decimal Total
);

public record CartItemDto(Guid Id, Guid ProductId, string ProductName, decimal UnitPrice, int Quantity);
