namespace NotificationService.Domain.Models;

public record Notification(
    Guid Id,
    Guid? UserId,
    string Type,
    string Channel,
    string Subject,
    string Body,
    string Status,
    DateTime CreatedAt,
    DateTime? SentAt
);

public record CreateNotificationRequest(
    Guid? UserId,
    string Type,
    string Channel,
    string Subject,
    string Body
);

public static class NotificationTypes
{
    public const string UserCreated = "user_created";
    public const string OrderConfirmed = "order_confirmed";
    public const string OrderCancelled = "order_cancelled";
    public const string PaymentProcessed = "payment_processed";
    public const string PaymentFailed = "payment_failed";
    public const string LowStock = "low_stock";
}

public static class NotificationChannels
{
    public const string Email = "email";
    public const string Sms = "sms";
    public const string Push = "push";
}
