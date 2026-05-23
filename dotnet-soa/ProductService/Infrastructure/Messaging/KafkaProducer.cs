using System.Text.Json;
using Confluent.Kafka;

namespace ProductService.Infrastructure.Messaging;

/// <summary>
/// Kafka message producer for publishing domain events to the ESB.
/// 
/// SOA Principle: Loose Coupling - services communicate through
/// asynchronous messaging rather than direct synchronous calls.
/// 
/// Design Pattern: Observer/Pub-Sub - events are published to topics
/// and consumed by interested services without direct dependencies.
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
    /// Messages are serialized to JSON for interoperability.
    /// </summary>
    /// <param name="topic">The Kafka topic to publish to</param>
    /// <param name="key">Message key for partitioning</param>
    /// <param name="message">The event object to serialize and publish</param>
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
