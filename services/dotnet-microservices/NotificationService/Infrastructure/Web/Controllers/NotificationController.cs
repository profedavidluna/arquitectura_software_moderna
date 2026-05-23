namespace NotificationService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using NotificationService.Domain.Interfaces;
using NotificationService.Domain.Models;
using NotificationService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class NotificationController : ControllerBase
{
    private readonly INotificationService _notificationService;

    public NotificationController(INotificationService notificationService) => _notificationService = notificationService;

    [HttpGet]
    public async Task<ActionResult<IEnumerable<NotificationDto>>> GetAll()
    {
        var notifications = await _notificationService.GetAllAsync();
        return Ok(notifications.Select(MapToDto));
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<NotificationDto>> GetById(Guid id)
    {
        var notification = await _notificationService.GetByIdAsync(id);
        if (notification == null) return NotFound();
        return Ok(MapToDto(notification));
    }

    [HttpGet("user/{userId:guid}")]
    public async Task<ActionResult<IEnumerable<NotificationDto>>> GetByUserId(Guid userId)
    {
        var notifications = await _notificationService.GetByUserIdAsync(userId);
        return Ok(notifications.Select(MapToDto));
    }

    [HttpPost]
    public async Task<ActionResult<NotificationDto>> Send([FromBody] CreateNotificationRequest request)
    {
        var notification = await _notificationService.SendNotificationAsync(request);
        return Created($"/api/notification/{notification.Id}", MapToDto(notification));
    }

    private static NotificationDto MapToDto(Notification n) => new(
        n.Id, n.UserId, n.Type, n.Channel, n.Subject, n.Body, n.Status, n.CreatedAt, n.SentAt
    );
}
