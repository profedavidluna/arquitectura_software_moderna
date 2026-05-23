namespace AnalyticsService.Application;

using AnalyticsService.Domain.Interfaces;
using AnalyticsService.Domain.Models;

public class AnalyticsServiceImpl : IAnalyticsService
{
    private readonly IAnalyticsRepository _repository;
    private readonly ILogger<AnalyticsServiceImpl> _logger;

    public AnalyticsServiceImpl(IAnalyticsRepository repository, ILogger<AnalyticsServiceImpl> logger)
    {
        _repository = repository;
        _logger = logger;
    }

    public async Task<AnalyticsEvent> RecordEventAsync(RecordEventRequest request)
    {
        var analyticsEvent = new AnalyticsEvent(
            Id: Guid.NewGuid(),
            EventType: request.EventType,
            Source: request.Source,
            Payload: request.Payload,
            CreatedAt: DateTime.UtcNow
        );

        var recorded = await _repository.RecordEventAsync(analyticsEvent);
        _logger.LogDebug("Recorded event: {EventType} from {Source}", request.EventType, request.Source);
        return recorded;
    }

    public async Task<Metric> RecordMetricAsync(RecordMetricRequest request)
    {
        var metric = new Metric(
            Id: Guid.NewGuid(),
            MetricName: request.MetricName,
            MetricValue: request.MetricValue,
            Dimensions: request.Dimensions,
            Timestamp: DateTime.UtcNow
        );

        return await _repository.RecordMetricAsync(metric);
    }

    public async Task<IEnumerable<AnalyticsEvent>> GetEventsAsync(int limit = 100) =>
        await _repository.GetEventsAsync(limit);

    public async Task<IEnumerable<AnalyticsEvent>> GetEventsByTypeAsync(string eventType) =>
        await _repository.GetEventsByTypeAsync(eventType);

    public async Task<MetricsSummary> GetSummaryAsync()
    {
        var totalEvents = await _repository.GetEventCountAsync();
        var totalOrders = await _repository.GetEventCountByTypeAsync("OrderCreated");
        var totalPayments = await _repository.GetEventCountByTypeAsync("PaymentProcessed");

        var revenueMetrics = await _repository.GetMetricsAsync("revenue");
        var totalRevenue = revenueMetrics.Sum(m => m.MetricValue);

        var events = await _repository.GetEventsAsync(1000);
        var eventsByType = events
            .GroupBy(e => e.EventType)
            .ToDictionary(g => g.Key, g => g.Count());

        return new MetricsSummary(totalEvents, totalOrders, totalPayments, totalRevenue, eventsByType);
    }
}
