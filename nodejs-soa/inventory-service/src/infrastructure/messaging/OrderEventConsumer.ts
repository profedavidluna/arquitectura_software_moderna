import { Kafka, Consumer } from 'kafkajs';
import { IInventoryService } from '../../domain/service/IInventoryService';
import { TOPICS } from './events';

/**
 * Order Event Consumer - Saga Pattern Participant.
 * 
 * Listens to order.created and order.cancelled topics.
 * - order.created → attempts to reserve stock
 * - order.cancelled → releases reserved stock (compensation)
 */
export class OrderEventConsumer {
  private consumer: Consumer;

  constructor(
    brokers: string[],
    groupId: string,
    private readonly inventoryService: IInventoryService
  ) {
    const kafka = new Kafka({ clientId: 'inventory-consumer', brokers });
    this.consumer = kafka.consumer({ groupId });
  }

  async start(): Promise<void> {
    await this.consumer.connect();
    await this.consumer.subscribe({ topic: TOPICS.ORDER_CREATED, fromBeginning: true });
    await this.consumer.subscribe({ topic: TOPICS.ORDER_CANCELLED, fromBeginning: true });

    await this.consumer.run({
      eachMessage: async ({ topic, message }) => {
        const value = JSON.parse(message.value?.toString() || '{}');
        console.log(`[Consumer] Received ${topic}:`, value.eventType || topic);

        try {
          switch (topic) {
            case TOPICS.ORDER_CREATED:
              await this.handleOrderCreated(value);
              break;
            case TOPICS.ORDER_CANCELLED:
              await this.handleOrderCancelled(value);
              break;
          }
        } catch (error) {
          console.error(`[Consumer] Error processing ${topic}:`, error);
        }
      },
    });

    console.log('[Consumer] Listening to order.created, order.cancelled');
  }

  private async handleOrderCreated(event: any): Promise<void> {
    const { orderId, productId, quantity } = event;
    console.log(`[Saga] Attempting stock reservation: orderId=${orderId}, productId=${productId}, qty=${quantity}`);
    await this.inventoryService.reserveStock(productId, quantity, orderId);
  }

  private async handleOrderCancelled(event: any): Promise<void> {
    const { orderId, productId, quantity } = event;
    console.log(`[Saga] Releasing stock: orderId=${orderId}, productId=${productId}, qty=${quantity}`);
    await this.inventoryService.releaseStock(productId, quantity, orderId);
  }

  async stop(): Promise<void> {
    await this.consumer.disconnect();
  }
}
