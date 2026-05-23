namespace UserService.Infrastructure.Messaging;

using Confluent.Kafka;

public class UserEventConsumer : BackgroundService
{
    private readonly ILogger<UserEventConsumer> _logger;
    private readonly IConfiguration _configuration;

    public UserEventConsumer(ILogger<UserEventConsumer> logger, IConfiguration configuration)
    {
        _logger = logger;
        _configuration = configuration;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var bootstrapServers = _configuration["Kafka:BootstrapServers"];
        if (string.IsNullOrEmpty(bootstrapServers))
        {
            _logger.LogWarning("Kafka not configured. UserEventConsumer disabled.");
            return;
        }

        await Task.Delay(5000, stoppingToken); // Wait for Kafka to be ready

        var config = new ConsumerConfig
        {
            BootstrapServers = bootstrapServers,
            GroupId = _configuration["Kafka:GroupId"] ?? "user-service-group",
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true
        };

        try
        {
            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            consumer.Subscribe("user-commands");

            _logger.LogInformation("UserEventConsumer started, listening on 'user-commands'");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromSeconds(1));
                    if (result != null)
                    {
                        _logger.LogInformation("Received user command: {Message}", result.Message.Value);
                        // Process command here
                    }
                }
                catch (ConsumeException ex)
                {
                    _logger.LogError(ex, "Error consuming message");
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "UserEventConsumer failed to start");
        }
    }
}
