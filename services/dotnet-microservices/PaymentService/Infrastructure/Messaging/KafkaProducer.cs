namespace PaymentService.Infrastructure.Messaging;

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
                var config = new ProducerConfig { BootstrapServers = bootstrapServers, Acks = Acks.Leader, MessageTimeoutMs = 5000 };
                _producer = new ProducerBuilder<string, string>(config).Build();
                _isEnabled = true;
            }
            catch (Exception ex) { _logger.LogWarning(ex, "Kafka init failed."); _isEnabled = false; }
        }
    }

    public async Task PublishAsync<T>(string topic, T message)
    {
        if (!_isEnabled || _producer == null) return;
        try
        {
            var json = JsonSerializer.Serialize(message);
            await _producer.ProduceAsync(topic, new Message<string, string> { Key = Guid.NewGuid().ToString(), Value = json });
        }
        catch (Exception ex) { _logger.LogError(ex, "Publish failed to {Topic}", topic); }
    }

    public void Dispose() => _producer?.Dispose();
}
