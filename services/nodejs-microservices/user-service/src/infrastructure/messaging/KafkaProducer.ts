/**
 * Kafka Producer
 * 
 * Publishes domain events to Kafka topics for asynchronous
 * communication between microservices (Event-Driven Architecture).
 * 
 * Events enable loose coupling - the producer doesn't need to know
 * which services consume the events.
 */
import { Kafka, Producer } from 'kafkajs';
import { config } from '../../config';

export class KafkaProducer {
  private producer: Producer;
  private connected = false;

  constructor() {
    const kafka = new Kafka({
      clientId: config.kafka.clientId,
      brokers: config.kafka.brokers,
      retry: {
        initialRetryTime: 1000,
        retries: 5,
      },
    });
    this.producer = kafka.producer();
  }

  async connect(): Promise<void> {
    try {
      await this.producer.connect();
      this.connected = true;
      console.log('[Kafka Producer] Connected successfully');
    } catch (error) {
      console.error('[Kafka Producer] Connection failed:', error);
      // Don't throw - service can still operate without Kafka
    }
  }

  async publish(topic: string, event: { type: string; data: any }): Promise<void> {
    if (!this.connected) {
      console.warn('[Kafka Producer] Not connected, skipping event:', event.type);
      return;
    }

    try {
      await this.producer.send({
        topic,
        messages: [
          {
            key: event.data.id || 'unknown',
            value: JSON.stringify({
              eventType: event.type,
              timestamp: new Date().toISOString(),
              source: config.serviceName,
              data: event.data,
            }),
          },
        ],
      });
      console.log(`[Kafka Producer] Published ${event.type} to ${topic}`);
    } catch (error) {
      console.error(`[Kafka Producer] Failed to publish ${event.type}:`, error);
    }
  }

  async disconnect(): Promise<void> {
    if (this.connected) {
      await this.producer.disconnect();
      this.connected = false;
    }
  }
}
