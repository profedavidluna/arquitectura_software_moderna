// =============================================================================
// Kafka Producer - Product Service
// =============================================================================
// The Kafka Producer publishes events to the Enterprise Service Bus (ESB).
// In SOA, services communicate asynchronously through the ESB, enabling:
// - Loose coupling: producer doesn't know who consumes the events
// - Reliability: messages are persisted in Kafka until consumed
// - Scalability: multiple consumers can process events in parallel
// =============================================================================

import { Kafka, Producer } from 'kafkajs';

export class KafkaProducer {
  private producer: Producer;

  constructor(brokers: string[], clientId: string) {
    const kafka = new Kafka({
      clientId,
      brokers,
      retry: {
        initialRetryTime: 1000,
        retries: 5,
      },
    });
    this.producer = kafka.producer();
  }

  /**
   * Connect to Kafka broker.
   * Should be called during service startup.
   */
  async connect(): Promise<void> {
    await this.producer.connect();
    console.log('[KafkaProducer] Connected to Kafka');
  }

  /**
   * Disconnect from Kafka broker.
   * Should be called during graceful shutdown.
   */
  async disconnect(): Promise<void> {
    await this.producer.disconnect();
    console.log('[KafkaProducer] Disconnected from Kafka');
  }

  /**
   * Publish an event to a Kafka topic.
   * @param topic - The Kafka topic (e.g., 'product.created')
   * @param key - Message key for partitioning (ensures ordering per entity)
   * @param value - The event payload (will be JSON serialized)
   */
  async publish(topic: string, key: string, value: object): Promise<void> {
    await this.producer.send({
      topic,
      messages: [
        {
          key,
          value: JSON.stringify(value),
          headers: {
            'content-type': 'application/json',
            'timestamp': Date.now().toString(),
          },
        },
      ],
    });
  }
}
