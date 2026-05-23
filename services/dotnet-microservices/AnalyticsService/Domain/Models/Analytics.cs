namespace AnalyticsService.Domain.Models;

public record AnalyticsEvent(
    Guid Id,
    string EventType,
    string Source,
    string? Payload,
    DateTime CreatedAt
);

public record Metric(
    Guid Id,
    string MetricName,
    decimal MetricValue,
    string? Dimensions,
    DateTime Timestamp
);

public record RecordEventRequest(string EventType, string Source, string? Payload);
public record RecordMetricRequest(string MetricName, decimal MetricValue, string? Dimensions);

public record MetricsSummary(
    int TotalEvents,
    int TotalOrders,
    int TotalPayments,
    decimal TotalRevenue,
    Dictionary<string, int> EventsByType
);
