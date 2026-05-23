import { Kafka, Producer } from 'kafkajs';
import { config } from '../../config';

export class KafkaProducer {
  private producer: Producer;
  private connected = false;

  constructor() {
    const kafka = new Kafka({ clientId: config.kafka.clientId, brokers: config.kafka.brokers });
    this.producer = kafka.producer();
  }

  async connect(): Promise<void> {
    try { await this.producer.connect(); this.connected = true; console.log('[Kafka] Connected'); }
    catch (e) { console.error('[Kafka] Connection failed:', e); }
  }

  async publish(topic: string, event: { type: string; data: any }): Promise<void> {
    if (!this.connected) return;
    try {
      await this.producer.send({
        topic,
        messages: [{ key: event.data.id || event.data.orderId || 'unknown', value: JSON.stringify({ eventType: event.type, timestamp: new Date().toISOString(), source: config.serviceName, data: event.data }) }],
      });
      console.log(`[Kafka] Published ${event.type} to ${topic}`);
    } catch (e) { console.error('[Kafka] Publish failed:', e); }
  }

  async disconnect(): Promise<void> { if (this.connected) await this.producer.disconnect(); }
}
