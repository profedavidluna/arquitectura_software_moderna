using Xunit;
using Moq;

namespace NotificationService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using NotificationService.Application;
using NotificationService.Domain.Models;
using NotificationService.Infrastructure.Persistence;
using NotificationService.Infrastructure.Persistence.Repositories;

public class NotificationServiceTests
{
    private readonly NotificationServiceImpl _service;

    public NotificationServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new AppDbContext(options);
        var repository = new NotificationRepository(context);
        _service = new NotificationServiceImpl(repository, NullLogger<NotificationServiceImpl>.Instance);
    }

    [Fact]
    public async Task SendNotification_ShouldCreateAndSend()
    {
        var request = new CreateNotificationRequest(Guid.NewGuid(), NotificationTypes.UserCreated, NotificationChannels.Email, "Welcome!", "Your account is ready.");

        var result = await _service.SendNotificationAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal("sent", result.Status);
        Assert.NotNull(result.SentAt);
        Assert.Equal("Welcome!", result.Subject);
    }

    [Fact]
    public async Task GetByUserId_ShouldReturnUserNotifications()
    {
        var userId = Guid.NewGuid();
        await _service.SendNotificationAsync(new CreateNotificationRequest(userId, "order_confirmed", "email", "Order 1", "Body 1"));
        await _service.SendNotificationAsync(new CreateNotificationRequest(userId, "payment_processed", "email", "Payment", "Body 2"));

        var results = await _service.GetByUserIdAsync(userId);

        Assert.Equal(2, results.Count());
    }

    [Fact]
    public async Task GetById_ShouldReturnNotification()
    {
        var created = await _service.SendNotificationAsync(new CreateNotificationRequest(Guid.NewGuid(), "test", "email", "Test", "Test body"));

        var found = await _service.GetByIdAsync(created.Id);

        Assert.NotNull(found);
        Assert.Equal(created.Id, found.Id);
    }
}

