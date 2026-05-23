namespace NotificationService.Application;

using NotificationService.Domain.Interfaces;
using NotificationService.Domain.Models;

public class NotificationServiceImpl : INotificationService
{
    private readonly INotificationRepository _repository;
    private readonly ILogger<NotificationServiceImpl> _logger;

    public NotificationServiceImpl(INotificationRepository repository, ILogger<NotificationServiceImpl> logger)
    {
        _repository = repository;
        _logger = logger;
    }

    public async Task<Notification?> GetByIdAsync(Guid id) => await _repository.GetByIdAsync(id);

    public async Task<IEnumerable<Notification>> GetByUserIdAsync(Guid userId) => await _repository.GetByUserIdAsync(userId);

    public async Task<IEnumerable<Notification>> GetAllAsync() => await _repository.GetAllAsync();

    public async Task<Notification> SendNotificationAsync(CreateNotificationRequest request)
    {
        var notification = new Notification(
            Id: Guid.NewGuid(),
            UserId: request.UserId,
            Type: request.Type,
            Channel: request.Channel,
            Subject: request.Subject,
            Body: request.Body,
            Status: "pending",
            CreatedAt: DateTime.UtcNow,
            SentAt: null
        );

        var created = await _repository.CreateAsync(notification);

        // Simulate sending notification
        _logger.LogInformation(
            "[{Channel}] Notification sent - To: {UserId}, Subject: {Subject}, Type: {Type}",
            request.Channel.ToUpper(), request.UserId, request.Subject, request.Type);

        var sent = await _repository.UpdateStatusAsync(created.Id, "sent", DateTime.UtcNow);
        return sent;
    }
}
