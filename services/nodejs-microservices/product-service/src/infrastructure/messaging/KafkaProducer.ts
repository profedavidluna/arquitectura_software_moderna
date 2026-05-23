import { Kafka, Producer } from 'kafkajs';
import { config } from '../../config';

export class KafkaProducer {
  private producer: Producer;
  private connected = false;

  constructor() {
    const kafka = new Kafka({
      clientId: config.kafka.clientId,
      brokers: config.kafka.brokers,
      retry: { initialRetryTime: 1000, retries: 5 },
    });
    this.producer = kafka.producer();
  }

  async connect(): Promise<void> {
    try {
      await this.producer.connect();
      this.connected = true;
      console.log('[Kafka Producer] Connected');
    } catch (error) {
      console.error('[Kafka Producer] Connection failed:', error);
    }
  }

  async publish(topic: string, event: { type: string; data: any }): Promise<void> {
    if (!this.connected) return;
    try {
      await this.producer.send({
        topic,
        messages: [{
          key: event.data.id || 'unknown',
          value: JSON.stringify({
            eventType: event.type,
            timestamp: new Date().toISOString(),
            source: config.serviceName,
            data: event.data,
          }),
        }],
      });
      console.log(`[Kafka] Published ${event.type} to ${topic}`);
    } catch (error) {
      console.error(`[Kafka] Failed to publish:`, error);
    }
  }

  async disconnect(): Promise<void> {
    if (this.connected) await this.producer.disconnect();
  }
}
