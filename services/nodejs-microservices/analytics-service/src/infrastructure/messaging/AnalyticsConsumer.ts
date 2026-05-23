/**
 * Analytics Event Consumer
 * 
 * Subscribes to ALL Kafka topics to build a complete picture of system activity.
 * This is a common pattern in event-driven architectures: a dedicated analytics
 * service that consumes all events for reporting and monitoring.
 * 
 * This service uses its own consumer group, so it doesn't interfere with
 * other services' event processing.
 */
import { Kafka, Consumer } from 'kafkajs';
import { config } from '../../config';
import { AnalyticsServiceImpl } from '../../application/AnalyticsServiceImpl';

export class AnalyticsConsumer {
  private consumer: Consumer;

  constructor(private readonly analyticsService: AnalyticsServiceImpl) {
    const kafka = new Kafka({ clientId: config.kafka.clientId, brokers: config.kafka.brokers });
    this.consumer = kafka.consumer({ groupId: config.kafka.groupId });
  }

  async start(): Promise<void> {
    try {
      await this.consumer.connect();

      // Subscribe to ALL event topics
      await this.consumer.subscribe({
        topics: ['user-events', 'product-events', 'order-events', 'payment-events', 'inventory-events'],
        fromBeginning: false,
      });

      await this.consumer.run({
        eachMessage: async ({ topic, message }) => {
          try {
            const event = JSON.parse(message.value?.toString() || '{}');
            await this.analyticsService.processEvent(event.eventType, event.source, event.data);
          } catch (error) {
            console.error('[Analytics Consumer] Error:', error);
          }
        },
      });

      console.log('[Consumer] Listening to ALL event topics for analytics');
    } catch (error) {
      console.error('[Consumer] Failed to start:', error);
    }
  }

  async stop(): Promise<void> {
    await this.consumer.disconnect();
  }
}
