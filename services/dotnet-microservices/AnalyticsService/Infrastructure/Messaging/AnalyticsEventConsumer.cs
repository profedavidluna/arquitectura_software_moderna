namespace AnalyticsService.Infrastructure.Messaging;

using System.Text.Json;
using Confluent.Kafka;
using AnalyticsService.Domain.Interfaces;
using AnalyticsService.Domain.Models;

public class AnalyticsEventConsumer : BackgroundService
{
    private readonly ILogger<AnalyticsEventConsumer> _logger;
    private readonly IConfiguration _configuration;
    private readonly IServiceProvider _serviceProvider;

    public AnalyticsEventConsumer(ILogger<AnalyticsEventConsumer> logger, IConfiguration configuration, IServiceProvider serviceProvider)
    {
        _logger = logger;
        _configuration = configuration;
        _serviceProvider = serviceProvider;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var bootstrapServers = _configuration["Kafka:BootstrapServers"];
        if (string.IsNullOrEmpty(bootstrapServers))
        {
            _logger.LogWarning("Kafka not configured. AnalyticsEventConsumer disabled.");
            return;
        }

        await Task.Delay(5000, stoppingToken);

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = _configuration["Kafka:GroupId"] ?? "analytics-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        try
        {
            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            consumer.Subscribe(new[] { "user-events", "product-events", "cart-events", "order-events", "payment-events", "inventory-events" });

            _logger.LogInformation("AnalyticsEventConsumer started, listening on all event topics");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromSeconds(1));
                    if (result != null) await ProcessEventAsync(result.Topic, result.Message.Value);
                }
                catch (ConsumeException ex) { _logger.LogError(ex, "Error consuming analytics event"); }
            }
        }
        catch (Exception ex) { _logger.LogError(ex, "AnalyticsEventConsumer failed"); }
    }

    private async Task ProcessEventAsync(string topic, string messageJson)
    {
        try
        {
            var message = JsonSerializer.Deserialize<JsonElement>(messageJson);
            var eventType = message.GetProperty("EventType").GetString() ?? "Unknown";

            using var scope = _serviceProvider.CreateScope();
            var analyticsService = scope.ServiceProvider.GetRequiredService<IAnalyticsService>();

            // Record the event
            await analyticsService.RecordEventAsync(new RecordEventRequest(eventType, topic, messageJson));

            // Record revenue metric for payment events
            if (eventType == "PaymentProcessed" && message.TryGetProperty("Amount", out var amountProp))
            {
                var amount = amountProp.GetDecimal();
                await analyticsService.RecordMetricAsync(new RecordMetricRequest("revenue", amount, $"{{\"source\":\"{topic}\"}}"));
            }

            _logger.LogDebug("Analytics recorded: {EventType} from {Topic}", eventType, topic);
        }
        catch (Exception ex) { _logger.LogError(ex, "Error processing analytics event"); }
    }
}
