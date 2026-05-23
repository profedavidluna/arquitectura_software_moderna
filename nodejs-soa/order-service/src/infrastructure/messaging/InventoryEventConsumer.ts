// =============================================================================
// Inventory Event Consumer - Order Service
// =============================================================================
// This consumer listens to events from the Inventory Service:
// - stock.reserved: Stock was successfully reserved → Confirm order
// - stock.insufficient: Not enough stock → Cancel order
//
// This is the "response" side of the Saga pattern.
// The Order Service (Saga Orchestrator) reacts to inventory responses
// and updates the order state accordingly.
// =============================================================================

import { Kafka, Consumer } from 'kafkajs';
import { IOrderService } from '../../domain/service/IOrderService';
import { StockReservedEvent, StockInsufficientEvent } from './events';

export class InventoryEventConsumer {
  private consumer: Consumer;

  constructor(
    brokers: string[],
    groupId: string,
    clientId: string,
    private readonly orderService: IOrderService
  ) {
    const kafka = new Kafka({
      clientId,
      brokers,
      retry: {
        initialRetryTime: 1000,
        retries: 5,
      },
    });
    this.consumer = kafka.consumer({ groupId });
  }

  /**
   * Subscribe to inventory events and start processing.
   */
  async start(): Promise<void> {
    await this.consumer.connect();
    console.log('[InventoryEventConsumer] Connected to Kafka');

    // Subscribe to inventory response topics
    await this.consumer.subscribe({ topic: 'stock.reserved', fromBeginning: true });
    await this.consumer.subscribe({ topic: 'stock.insufficient', fromBeginning: true });

    // Process incoming messages
    await this.consumer.run({
      eachMessage: async ({ topic, message }) => {
        const value = JSON.parse(message.value?.toString() || '{}');
        console.log(`[InventoryEventConsumer] Received event on topic: ${topic}`);

        try {
          await this.handleMessage(topic, value);
        } catch (error) {
          console.error(`[InventoryEventConsumer] Error processing message:`, error);
        }
      },
    });

    console.log('[InventoryEventConsumer] Listening for inventory events...');
  }

  /**
   * Route messages to appropriate handlers based on topic.
   */
  private async handleMessage(topic: string, message: any): Promise<void> {
    switch (topic) {
      case 'stock.reserved':
        await this.handleStockReserved(message as StockReservedEvent);
        break;
      case 'stock.insufficient':
        await this.handleStockInsufficient(message as StockInsufficientEvent);
        break;
      default:
        console.warn(`[InventoryEventConsumer] Unknown topic: ${topic}`);
    }
  }

  /**
   * Handle stock.reserved event - Confirm the order.
   * This completes the Saga successfully.
   */
  private async handleStockReserved(event: StockReservedEvent): Promise<void> {
    console.log(`[InventoryEventConsumer] Stock reserved for order: ${event.data.orderId}`);
    await this.orderService.confirm(event.data.orderId);
    console.log(`[InventoryEventConsumer] Order ${event.data.orderId} confirmed - Saga completed ✓`);
  }

  /**
   * Handle stock.insufficient event - Cancel the order.
   * This is the Saga failure path.
   */
  private async handleStockInsufficient(event: StockInsufficientEvent): Promise<void> {
    console.log(
      `[InventoryEventConsumer] Insufficient stock for order: ${event.data.orderId}` +
      ` (product: ${event.data.productId}, requested: ${event.data.requestedQuantity}, available: ${event.data.availableQuantity})`
    );
    await this.orderService.markInsufficientStock(event.data.orderId);
    console.log(`[InventoryEventConsumer] Order ${event.data.orderId} cancelled - insufficient stock ✗`);
  }

  /**
   * Disconnect the consumer (graceful shutdown).
   */
  async stop(): Promise<void> {
    await this.consumer.disconnect();
    console.log('[InventoryEventConsumer] Disconnected from Kafka');
  }
}
