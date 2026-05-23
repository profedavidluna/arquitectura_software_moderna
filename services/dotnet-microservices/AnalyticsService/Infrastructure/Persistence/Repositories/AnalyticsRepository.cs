namespace AnalyticsService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using AnalyticsService.Domain.Interfaces;
using AnalyticsService.Domain.Models;
using AnalyticsService.Infrastructure.Persistence.Entities;

public class AnalyticsRepository : IAnalyticsRepository
{
    private readonly AppDbContext _context;

    public AnalyticsRepository(AppDbContext context) => _context = context;

    public async Task<AnalyticsEvent> RecordEventAsync(AnalyticsEvent analyticsEvent)
    {
        var entity = new EventEntity
        {
            Id = analyticsEvent.Id, EventType = analyticsEvent.EventType,
            Source = analyticsEvent.Source, Payload = analyticsEvent.Payload,
            CreatedAt = analyticsEvent.CreatedAt
        };
        _context.Events.Add(entity);
        await _context.SaveChangesAsync();
        return MapEventToDomain(entity);
    }

    public async Task<Metric> RecordMetricAsync(Metric metric)
    {
        var entity = new MetricEntity
        {
            Id = metric.Id, MetricName = metric.MetricName,
            MetricValue = metric.MetricValue, Dimensions = metric.Dimensions,
            Timestamp = metric.Timestamp
        };
        _context.Metrics.Add(entity);
        await _context.SaveChangesAsync();
        return MapMetricToDomain(entity);
    }

    public async Task<IEnumerable<AnalyticsEvent>> GetEventsAsync(int limit = 100)
    {
        var entities = await _context.Events.OrderByDescending(e => e.CreatedAt).Take(limit).ToListAsync();
        return entities.Select(MapEventToDomain);
    }

    public async Task<IEnumerable<AnalyticsEvent>> GetEventsByTypeAsync(string eventType)
    {
        var entities = await _context.Events.Where(e => e.EventType == eventType).OrderByDescending(e => e.CreatedAt).ToListAsync();
        return entities.Select(MapEventToDomain);
    }

    public async Task<IEnumerable<Metric>> GetMetricsAsync(string metricName)
    {
        var entities = await _context.Metrics.Where(m => m.MetricName == metricName).ToListAsync();
        return entities.Select(MapMetricToDomain);
    }

    public async Task<int> GetEventCountAsync() => await _context.Events.CountAsync();

    public async Task<int> GetEventCountByTypeAsync(string eventType) =>
        await _context.Events.CountAsync(e => e.EventType == eventType);

    private static AnalyticsEvent MapEventToDomain(EventEntity e) => new(e.Id, e.EventType, e.Source, e.Payload, e.CreatedAt);
    private static Metric MapMetricToDomain(MetricEntity e) => new(e.Id, e.MetricName, e.MetricValue, e.Dimensions, e.Timestamp);
}
