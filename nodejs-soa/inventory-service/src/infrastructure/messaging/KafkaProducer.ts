import { Kafka, Producer } from 'kafkajs';

/**
 * Kafka Producer - publishes events to the Enterprise Service Bus.
 * Observer Pattern: Services subscribe to topics independently.
 */
export class KafkaProducer {
  private producer: Producer;

  constructor(brokers: string[], clientId: string) {
    const kafka = new Kafka({ clientId, brokers });
    this.producer = kafka.producer();
  }

  async connect(): Promise<void> {
    await this.producer.connect();
    console.log('[Kafka Producer] Connected');
  }

  async disconnect(): Promise<void> {
    await this.producer.disconnect();
  }

  async publish(topic: string, key: string, value: object): Promise<void> {
    await this.producer.send({
      topic,
      messages: [{ key, value: JSON.stringify(value) }],
    });
    console.log(`[Kafka] Published to ${topic}: key=${key}`);
  }
}
