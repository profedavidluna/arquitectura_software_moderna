namespace AnalyticsService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using AnalyticsService.Domain.Interfaces;
using AnalyticsService.Domain.Models;
using AnalyticsService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class AnalyticsController : ControllerBase
{
    private readonly IAnalyticsService _analyticsService;

    public AnalyticsController(IAnalyticsService analyticsService) => _analyticsService = analyticsService;

    [HttpGet("events")]
    public async Task<ActionResult<IEnumerable<EventDto>>> GetEvents([FromQuery] int limit = 100)
    {
        var events = await _analyticsService.GetEventsAsync(limit);
        return Ok(events.Select(e => new EventDto(e.Id, e.EventType, e.Source, e.Payload, e.CreatedAt)));
    }

    [HttpGet("events/{eventType}")]
    public async Task<ActionResult<IEnumerable<EventDto>>> GetEventsByType(string eventType)
    {
        var events = await _analyticsService.GetEventsByTypeAsync(eventType);
        return Ok(events.Select(e => new EventDto(e.Id, e.EventType, e.Source, e.Payload, e.CreatedAt)));
    }

    [HttpGet("summary")]
    public async Task<ActionResult<MetricsSummary>> GetSummary()
    {
        var summary = await _analyticsService.GetSummaryAsync();
        return Ok(summary);
    }

    [HttpPost("events")]
    public async Task<ActionResult<EventDto>> RecordEvent([FromBody] RecordEventRequest request)
    {
        var recorded = await _analyticsService.RecordEventAsync(request);
        return Created($"/api/analytics/events", new EventDto(recorded.Id, recorded.EventType, recorded.Source, recorded.Payload, recorded.CreatedAt));
    }

    [HttpPost("metrics")]
    public async Task<ActionResult<MetricDto>> RecordMetric([FromBody] RecordMetricRequest request)
    {
        var recorded = await _analyticsService.RecordMetricAsync(request);
        return Created($"/api/analytics/metrics", new MetricDto(recorded.Id, recorded.MetricName, recorded.MetricValue, recorded.Dimensions, recorded.Timestamp));
    }
}
