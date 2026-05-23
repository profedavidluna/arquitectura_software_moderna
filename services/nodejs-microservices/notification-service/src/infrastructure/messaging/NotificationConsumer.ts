/**
 * Notification Event Consumer
 * 
 * Subscribes to multiple Kafka topics and routes events to the
 * appropriate notification handler. This demonstrates the
 * Event-Driven Architecture pattern where services react to
 * domain events without direct coupling.
 * 
 * Topics consumed:
 * - user-events: Welcome emails
 * - order-events: Order confirmations/cancellations
 * - payment-events: Payment receipts/refunds
 */
import { Kafka, Consumer } from 'kafkajs';
import { config } from '../../config';
import { NotificationServiceImpl } from '../../application/NotificationServiceImpl';

export class NotificationConsumer {
  private consumer: Consumer;

  constructor(private readonly notificationService: NotificationServiceImpl) {
    const kafka = new Kafka({ clientId: config.kafka.clientId, brokers: config.kafka.brokers });
    this.consumer = kafka.consumer({ groupId: config.kafka.groupId });
  }

  async start(): Promise<void> {
    try {
      await this.consumer.connect();

      // Subscribe to all relevant topics
      await this.consumer.subscribe({
        topics: ['user-events', 'order-events', 'payment-events'],
        fromBeginning: false,
      });

      await this.consumer.run({
        eachMessage: async ({ topic, message }) => {
          try {
            const event = JSON.parse(message.value?.toString() || '{}');
            console.log(`[Consumer] Received ${event.eventType} from ${topic}`);

            await this.routeEvent(event);
          } catch (error) {
            console.error('[Consumer] Error processing message:', error);
          }
        },
      });

      console.log('[Consumer] Listening for user, order, and payment events');
    } catch (error) {
      console.error('[Consumer] Failed to start:', error);
    }
  }

  private async routeEvent(event: any): Promise<void> {
    switch (event.eventType) {
      // User events
      case 'USER_CREATED':
        await this.notificationService.handleUserCreated(event.data);
        break;

      // Order events
      case 'ORDER_CONFIRMED':
        await this.notificationService.handleOrderConfirmed(event.data);
        break;
      case 'ORDER_CANCELLED':
        await this.notificationService.handleOrderCancelled(event.data);
        break;

      // Payment events
      case 'PAYMENT_COMPLETED':
        await this.notificationService.handlePaymentCompleted(event.data);
        break;
      case 'PAYMENT_REFUNDED':
        await this.notificationService.handlePaymentRefunded(event.data);
        break;

      default:
        console.log(`[Consumer] Unhandled event type: ${event.eventType}`);
    }
  }

  async stop(): Promise<void> {
    await this.consumer.disconnect();
  }
}
