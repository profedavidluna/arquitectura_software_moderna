namespace NotificationService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using NotificationService.Domain.Interfaces;
using NotificationService.Domain.Models;
using NotificationService.Infrastructure.Persistence.Entities;

public class NotificationRepository : INotificationRepository
{
    private readonly AppDbContext _context;

    public NotificationRepository(AppDbContext context) => _context = context;

    public async Task<Notification?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Notifications.FindAsync(id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<IEnumerable<Notification>> GetByUserIdAsync(Guid userId)
    {
        var entities = await _context.Notifications.Where(n => n.UserId == userId).OrderByDescending(n => n.CreatedAt).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<IEnumerable<Notification>> GetAllAsync()
    {
        var entities = await _context.Notifications.OrderByDescending(n => n.CreatedAt).Take(100).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<Notification> CreateAsync(Notification notification)
    {
        var entity = new NotificationEntity
        {
            Id = notification.Id, UserId = notification.UserId, Type = notification.Type,
            Channel = notification.Channel, Subject = notification.Subject, Body = notification.Body,
            Status = notification.Status, CreatedAt = notification.CreatedAt, SentAt = notification.SentAt
        };
        _context.Notifications.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Notification> UpdateStatusAsync(Guid id, string status, DateTime? sentAt)
    {
        var entity = await _context.Notifications.FindAsync(id)
            ?? throw new KeyNotFoundException($"Notification {id} not found");
        entity.Status = status;
        entity.SentAt = sentAt;
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    private static Notification MapToDomain(NotificationEntity e) => new(
        e.Id, e.UserId, e.Type, e.Channel, e.Subject, e.Body, e.Status, e.CreatedAt, e.SentAt
    );
}
