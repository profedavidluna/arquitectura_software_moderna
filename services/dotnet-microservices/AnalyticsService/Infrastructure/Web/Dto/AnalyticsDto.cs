namespace AnalyticsService.Infrastructure.Web.Dto;

public record EventDto(Guid Id, string EventType, string Source, string? Payload, DateTime CreatedAt);
public record MetricDto(Guid Id, string MetricName, decimal MetricValue, string? Dimensions, DateTime Timestamp);
