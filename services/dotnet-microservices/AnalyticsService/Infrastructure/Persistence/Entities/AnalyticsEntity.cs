namespace AnalyticsService.Infrastructure.Persistence.Entities;

public class EventEntity
{
    public Guid Id { get; set; }
    public string EventType { get; set; } = string.Empty;
    public string Source { get; set; } = string.Empty;
    public string? Payload { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public class MetricEntity
{
    public Guid Id { get; set; }
    public string MetricName { get; set; } = string.Empty;
    public decimal MetricValue { get; set; }
    public string? Dimensions { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}
