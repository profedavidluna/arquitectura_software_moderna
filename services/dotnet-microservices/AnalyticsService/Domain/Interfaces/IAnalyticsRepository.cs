namespace AnalyticsService.Domain.Interfaces;

using AnalyticsService.Domain.Models;

public interface IAnalyticsRepository
{
    Task<AnalyticsEvent> RecordEventAsync(AnalyticsEvent analyticsEvent);
    Task<Metric> RecordMetricAsync(Metric metric);
    Task<IEnumerable<AnalyticsEvent>> GetEventsAsync(int limit = 100);
    Task<IEnumerable<AnalyticsEvent>> GetEventsByTypeAsync(string eventType);
    Task<IEnumerable<Metric>> GetMetricsAsync(string metricName);
    Task<int> GetEventCountAsync();
    Task<int> GetEventCountByTypeAsync(string eventType);
}

public interface IAnalyticsService
{
    Task<AnalyticsEvent> RecordEventAsync(RecordEventRequest request);
    Task<Metric> RecordMetricAsync(RecordMetricRequest request);
    Task<IEnumerable<AnalyticsEvent>> GetEventsAsync(int limit = 100);
    Task<IEnumerable<AnalyticsEvent>> GetEventsByTypeAsync(string eventType);
    Task<MetricsSummary> GetSummaryAsync();
}
