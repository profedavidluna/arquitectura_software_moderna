namespace UserService.Infrastructure.Messaging;

using System.Text.Json;
using Confluent.Kafka;

public class KafkaProducer : IDisposable
{
    private readonly IProducer<string, string>? _producer;
    private readonly ILogger<KafkaProducer> _logger;
    private readonly bool _isEnabled;

    public KafkaProducer(IConfiguration configuration, ILogger<KafkaProducer> logger)
    {
        _logger = logger;
        var bootstrapServers = configuration["Kafka:BootstrapServers"];

        if (!string.IsNullOrEmpty(bootstrapServers))
        {
            try
            {
                var config = new ProducerConfig
                {
                    BootstrapServers = bootstrapServers,
                    Acks = Acks.Leader,
                    MessageTimeoutMs = 5000
                };
                _producer = new ProducerBuilder<string, string>(config).Build();
                _isEnabled = true;
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Kafka producer initialization failed. Running without Kafka.");
                _isEnabled = false;
            }
        }
    }

    public async Task PublishAsync<T>(string topic, T message)
    {
        if (!_isEnabled || _producer == null)
        {
            _logger.LogDebug("Kafka not available. Skipping publish to {Topic}", topic);
            return;
        }

        try
        {
            var json = JsonSerializer.Serialize(message);
            var kafkaMessage = new Message<string, string>
            {
                Key = Guid.NewGuid().ToString(),
                Value = json
            };

            await _producer.ProduceAsync(topic, kafkaMessage);
            _logger.LogInformation("Published message to {Topic}", topic);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish message to {Topic}", topic);
        }
    }

    public void Dispose()
    {
        _producer?.Dispose();
    }
}
