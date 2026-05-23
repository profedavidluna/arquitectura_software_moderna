/**
 * Inventory Event Consumer
 * 
 * Listens for order-events to automatically reserve/release stock.
 * This is the reactive side of the Saga pattern - inventory-service
 * responds to order lifecycle events.
 */
import { Kafka, Consumer } from 'kafkajs';
import { config } from '../../config';
import { InventoryServiceImpl } from '../../application/InventoryServiceImpl';

export class InventoryEventConsumer {
  private consumer: Consumer;

  constructor(private readonly inventoryService: InventoryServiceImpl) {
    const kafka = new Kafka({ clientId: config.kafka.clientId, brokers: config.kafka.brokers });
    this.consumer = kafka.consumer({ groupId: config.kafka.groupId });
  }

  async start(): Promise<void> {
    try {
      await this.consumer.connect();
      await this.consumer.subscribe({ topics: ['order-events'], fromBeginning: false });

      await this.consumer.run({
        eachMessage: async ({ message }) => {
          try {
            const event = JSON.parse(message.value?.toString() || '{}');
            console.log(`[Consumer] Received ${event.eventType}`);

            switch (event.eventType) {
              case 'ORDER_CONFIRMED':
                // Confirm reservations (stock already deducted)
                await this.inventoryService.confirmReservation(event.data.orderId);
                break;
              case 'ORDER_CANCELLED':
              case 'ORDER_FAILED':
                // Release reserved stock back to available
                await this.handleOrderCancelled(event.data);
                break;
            }
          } catch (error) {
            console.error('[Consumer] Error:', error);
          }
        },
      });

      console.log('[Consumer] Listening for order events');
    } catch (error) {
      console.error('[Consumer] Failed to start:', error);
    }
  }

  private async handleOrderCancelled(data: any): Promise<void> {
    if (data.items) {
      for (const item of data.items) {
        await this.inventoryService.releaseStock(item.productId, data.orderId);
      }
    }
  }

  async stop(): Promise<void> {
    await this.consumer.disconnect();
  }
}
