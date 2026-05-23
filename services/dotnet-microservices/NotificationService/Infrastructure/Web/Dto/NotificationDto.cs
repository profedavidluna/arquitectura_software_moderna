namespace NotificationService.Infrastructure.Web.Dto;

public record NotificationDto(Guid Id, Guid? UserId, string Type, string Channel, string Subject, string Body, string Status, DateTime CreatedAt, DateTime? SentAt);
