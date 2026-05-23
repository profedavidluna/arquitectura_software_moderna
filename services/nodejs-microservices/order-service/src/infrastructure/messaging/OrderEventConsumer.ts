/**
 * Order Event Consumer
 * 
 * Listens for events from inventory-service and payment-service
 * to advance or compensate the Saga.
 */
import { Kafka, Consumer } from 'kafkajs';
import { config } from '../../config';
import { OrderRepository } from '../persistence/OrderRepository';
import { OrderStatus, SagaStep, SagaStatus } from '../../domain/models/Order';

export class OrderEventConsumer {
  private consumer: Consumer;

  constructor(private readonly orderRepository: OrderRepository) {
    const kafka = new Kafka({ clientId: config.kafka.clientId, brokers: config.kafka.brokers });
    this.consumer = kafka.consumer({ groupId: config.kafka.groupId });
  }

  async start(): Promise<void> {
    try {
      await this.consumer.connect();
      await this.consumer.subscribe({ topics: ['inventory-events', 'payment-events'], fromBeginning: false });

      await this.consumer.run({
        eachMessage: async ({ topic, message }) => {
          try {
            const event = JSON.parse(message.value?.toString() || '{}');
            console.log(`[Consumer] Received ${event.eventType} from ${topic}`);

            switch (event.eventType) {
              case 'INVENTORY_RESERVED':
                await this.handleInventoryReserved(event.data);
                break;
              case 'INVENTORY_RESERVATION_FAILED':
                await this.handleInventoryFailed(event.data);
                break;
              case 'PAYMENT_COMPLETED':
                await this.handlePaymentCompleted(event.data);
                break;
              case 'PAYMENT_FAILED':
                await this.handlePaymentFailed(event.data);
                break;
            }
          } catch (error) {
            console.error('[Consumer] Error processing message:', error);
          }
        },
      });

      console.log('[Consumer] Listening for inventory and payment events');
    } catch (error) {
      console.error('[Consumer] Failed to start:', error);
    }
  }

  private async handleInventoryReserved(data: any): Promise<void> {
    await this.orderRepository.updateStatus(data.orderId, OrderStatus.INVENTORY_RESERVED);
    console.log(`[Saga] Order ${data.orderId}: Inventory reserved, proceeding to payment`);
  }

  private async handleInventoryFailed(data: any): Promise<void> {
    await this.orderRepository.updateStatus(data.orderId, OrderStatus.FAILED);
    console.log(`[Saga] Order ${data.orderId}: Inventory reservation failed, order cancelled`);
  }

  private async handlePaymentCompleted(data: any): Promise<void> {
    await this.orderRepository.updateStatus(data.orderId, OrderStatus.CONFIRMED, data.paymentId);
    console.log(`[Saga] Order ${data.orderId}: Payment completed, order confirmed!`);
  }

  private async handlePaymentFailed(data: any): Promise<void> {
    // Compensation: release inventory reservation
    await this.orderRepository.updateStatus(data.orderId, OrderStatus.FAILED);
    console.log(`[Saga] Order ${data.orderId}: Payment failed, triggering compensation`);
  }

  async stop(): Promise<void> {
    await this.consumer.disconnect();
  }
}
