namespace NotificationService.Infrastructure.Messaging;

using System.Text.Json;
using Confluent.Kafka;
using NotificationService.Domain.Interfaces;
using NotificationService.Domain.Models;

public class NotificationEventConsumer : BackgroundService
{
    private readonly ILogger<NotificationEventConsumer> _logger;
    private readonly IConfiguration _configuration;
    private readonly IServiceProvider _serviceProvider;

    public NotificationEventConsumer(ILogger<NotificationEventConsumer> logger, IConfiguration configuration, IServiceProvider serviceProvider)
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
            _logger.LogWarning("Kafka not configured. NotificationEventConsumer disabled.");
            return;
        }

        await Task.Delay(5000, stoppingToken);

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = _configuration["Kafka:GroupId"] ?? "notification-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        try
        {
            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            consumer.Subscribe(new[] { "user-events", "order-events", "payment-events", "inventory-events" });

            _logger.LogInformation("NotificationEventConsumer started, listening on all event topics");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromSeconds(1));
                    if (result != null) await ProcessEventAsync(result.Message.Value);
                }
                catch (ConsumeException ex) { _logger.LogError(ex, "Error consuming notification event"); }
            }
        }
        catch (Exception ex) { _logger.LogError(ex, "NotificationEventConsumer failed"); }
    }

    private async Task ProcessEventAsync(string messageJson)
    {
        try
        {
            var message = JsonSerializer.Deserialize<JsonElement>(messageJson);
            var eventType = message.GetProperty("EventType").GetString() ?? "Unknown";

            Guid? userId = null;
            if (message.TryGetProperty("UserId", out var userIdProp) && userIdProp.ValueKind == JsonValueKind.String)
            {
                userId = Guid.TryParse(userIdProp.GetString(), out var parsed) ? parsed : null;
            }

            var (subject, body) = eventType switch
            {
                "UserCreated" => ("Welcome!", $"Your account has been created successfully."),
                "OrderCreated" => ("Order Placed", $"Your order has been placed and is being processed."),
                "OrderConfirmed" => ("Order Confirmed", $"Your order has been confirmed and will be shipped soon."),
                "PaymentProcessed" => ("Payment Successful", $"Your payment has been processed successfully."),
                "PaymentFailed" => ("Payment Failed", $"Your payment could not be processed. Please try again."),
                "PaymentRefunded" => ("Refund Processed", $"Your refund has been processed."),
                "LowStock" => ("Low Stock Alert", $"A product is running low on stock."),
                _ => ($"Event: {eventType}", $"Event {eventType} occurred.")
            };

            using var scope = _serviceProvider.CreateScope();
            var notificationService = scope.ServiceProvider.GetRequiredService<INotificationService>();

            await notificationService.SendNotificationAsync(new CreateNotificationRequest(
                userId, eventType, NotificationChannels.Email, subject, body
            ));
        }
        catch (Exception ex) { _logger.LogError(ex, "Error processing notification event"); }
    }
}
