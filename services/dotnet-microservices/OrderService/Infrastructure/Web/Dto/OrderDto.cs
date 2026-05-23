namespace OrderService.Infrastructure.Web.Dto;

public record OrderDto(Guid Id, Guid UserId, string Status, decimal TotalAmount, string? ShippingAddress, string SagaState, List<OrderItemDto> Items, DateTime CreatedAt);
public record OrderItemDto(Guid Id, Guid ProductId, string ProductName, decimal UnitPrice, int Quantity);
public record UpdateStatusRequest(string Status);
