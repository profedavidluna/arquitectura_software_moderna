using System.Text.Json;
using Confluent.Kafka;

namespace OrderService.Infrastructure.Messaging;

/// <summary>
/// Kafka message producer for the Order Service.
/// Publishes order lifecycle events to the ESB for Saga coordination.
/// </summary>
public class KafkaProducer : IDisposable
{
    private readonly IProducer<string, string> _producer;

    public KafkaProducer(string bootstrapServers)
    {
        var config = new ProducerConfig
        {
            BootstrapServers = bootstrapServers,
            Acks = Acks.All,
            EnableIdempotence = true
        };
        _producer = new ProducerBuilder<string, string>(config).Build();
    }

    /// <summary>
    /// Publishes a message to the specified Kafka topic.
    /// </summary>
    public virtual async Task PublishAsync(string topic, string key, object message)
    {
        var json = JsonSerializer.Serialize(message, new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase
        });

        await _producer.ProduceAsync(topic, new Message<string, string>
        {
            Key = key,
            Value = json
        });
    }

    public void Dispose()
    {
        _producer?.Flush(TimeSpan.FromSeconds(5));
        _producer?.Dispose();
    }
}
