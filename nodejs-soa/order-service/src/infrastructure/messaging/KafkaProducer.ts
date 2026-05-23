// =============================================================================
// Kafka Producer - Order Service
// =============================================================================
// Publishes order events to the ESB for other services to consume.
// Events: order.created, order.confirmed, order.cancelled
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

  async connect(): Promise<void> {
    await this.producer.connect();
    console.log('[KafkaProducer] Connected to Kafka');
  }

  async disconnect(): Promise<void> {
    await this.producer.disconnect();
    console.log('[KafkaProducer] Disconnected from Kafka');
  }

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
