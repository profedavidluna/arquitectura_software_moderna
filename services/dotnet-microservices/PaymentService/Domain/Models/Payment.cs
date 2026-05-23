namespace PaymentService.Domain.Models;

public record Payment(
    Guid Id,
    Guid OrderId,
    Guid UserId,
    decimal Amount,
    string Currency,
    string Status,
    string? PaymentMethod,
    string? TransactionId,
    string? FailureReason,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

public record Refund(
    Guid Id,
    Guid PaymentId,
    decimal Amount,
    string? Reason,
    string Status,
    DateTime CreatedAt
);

public record ProcessPaymentRequest(
    Guid OrderId,
    Guid UserId,
    decimal Amount,
    string? PaymentMethod,
    string Currency = "USD"
);

public record RefundRequest(Guid PaymentId, decimal Amount, string? Reason);

public static class PaymentStatuses
{
    public const string Pending = "pending";
    public const string Processing = "processing";
    public const string Completed = "completed";
    public const string Failed = "failed";
    public const string Refunded = "refunded";
}
