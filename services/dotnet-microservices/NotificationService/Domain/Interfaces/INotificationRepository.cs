namespace NotificationService.Domain.Interfaces;

using NotificationService.Domain.Models;

public interface INotificationRepository
{
    Task<Notification?> GetByIdAsync(Guid id);
    Task<IEnumerable<Notification>> GetByUserIdAsync(Guid userId);
    Task<IEnumerable<Notification>> GetAllAsync();
    Task<Notification> CreateAsync(Notification notification);
    Task<Notification> UpdateStatusAsync(Guid id, string status, DateTime? sentAt);
}

public interface INotificationService
{
    Task<Notification?> GetByIdAsync(Guid id);
    Task<IEnumerable<Notification>> GetByUserIdAsync(Guid userId);
    Task<IEnumerable<Notification>> GetAllAsync();
    Task<Notification> SendNotificationAsync(CreateNotificationRequest request);
}
