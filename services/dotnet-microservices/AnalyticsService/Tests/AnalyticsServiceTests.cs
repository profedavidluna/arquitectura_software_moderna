using Xunit;
using Moq;

namespace AnalyticsService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using AnalyticsService.Application;
using AnalyticsService.Domain.Models;
using AnalyticsService.Infrastructure.Persistence;
using AnalyticsService.Infrastructure.Persistence.Repositories;

public class AnalyticsServiceTests
{
    private readonly AnalyticsServiceImpl _service;

    public AnalyticsServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new AppDbContext(options);
        var repository = new AnalyticsRepository(context);
        _service = new AnalyticsServiceImpl(repository, NullLogger<AnalyticsServiceImpl>.Instance);
    }

    [Fact]
    public async Task RecordEvent_ShouldStoreEvent()
    {
        var request = new RecordEventRequest("OrderCreated", "order-events", "{\"orderId\":\"123\"}");

        var result = await _service.RecordEventAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal("OrderCreated", result.EventType);
        Assert.Equal("order-events", result.Source);
    }

    [Fact]
    public async Task RecordMetric_ShouldStoreMetric()
    {
        var request = new RecordMetricRequest("revenue", 99.99m, "{\"source\":\"payment-events\"}");

        var result = await _service.RecordMetricAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal("revenue", result.MetricName);
        Assert.Equal(99.99m, result.MetricValue);
    }

    [Fact]
    public async Task GetEventsByType_ShouldFilterCorrectly()
    {
        await _service.RecordEventAsync(new RecordEventRequest("OrderCreated", "order-events", null));
        await _service.RecordEventAsync(new RecordEventRequest("UserCreated", "user-events", null));
        await _service.RecordEventAsync(new RecordEventRequest("OrderCreated", "order-events", null));

        var results = await _service.GetEventsByTypeAsync("OrderCreated");

        Assert.Equal(2, results.Count());
    }

    [Fact]
    public async Task GetSummary_ShouldAggregateMetrics()
    {
        await _service.RecordEventAsync(new RecordEventRequest("OrderCreated", "order-events", null));
        await _service.RecordEventAsync(new RecordEventRequest("PaymentProcessed", "payment-events", null));
        await _service.RecordMetricAsync(new RecordMetricRequest("revenue", 100m, null));
        await _service.RecordMetricAsync(new RecordMetricRequest("revenue", 200m, null));

        var summary = await _service.GetSummaryAsync();

        Assert.Equal(2, summary.TotalEvents);
        Assert.Equal(1, summary.TotalOrders);
        Assert.Equal(1, summary.TotalPayments);
        Assert.Equal(300m, summary.TotalRevenue);
    }
}

