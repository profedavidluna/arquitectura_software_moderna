namespace PaymentService.Infrastructure.Web.Dto;

public record PaymentDto(Guid Id, Guid OrderId, Guid UserId, decimal Amount, string Currency, string Status, string? PaymentMethod, string? TransactionId, string? FailureReason, DateTime CreatedAt);
public record RefundDto(Guid Id, Guid PaymentId, decimal Amount, string? Reason, string Status, DateTime CreatedAt);
