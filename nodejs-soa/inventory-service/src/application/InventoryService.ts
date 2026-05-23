import { v4 as uuidv4 } from 'uuid';
import { InventoryItem } from '../domain/model/InventoryItem';
import { IInventoryService } from '../domain/service/IInventoryService';
import { InventoryRepository } from '../infrastructure/persistence/InventoryRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { TOPICS, createStockReservedEvent, createStockInsufficientEvent, createStockReleasedEvent } from '../infrastructure/messaging/events';

/**
 * Inventory Service Implementation.
 * 
 * SRP: Orchestrates inventory operations between persistence and messaging.
 * Saga Pattern: Handles reserve (forward) and release (compensate) actions.
 */
export class InventoryServiceImpl implements IInventoryService {
  constructor(
    private readonly repository: InventoryRepository,
    private readonly producer: KafkaProducer
  ) {}

  async createItem(productId: string, productName: string, quantity: number): Promise<InventoryItem> {
    const item = new InventoryItem({
      id: uuidv4(),
      productId,
      productName,
      quantityAvailable: quantity,
      quantityReserved: 0,
      updatedAt: new Date(),
    });
    return this.repository.save(item);
  }

  async getByProductId(productId: string): Promise<InventoryItem | null> {
    return this.repository.findByProductId(productId);
  }

  async getAll(): Promise<InventoryItem[]> {
    return this.repository.findAll();
  }

  async reserveStock(productId: string, quantity: number, orderId: string): Promise<boolean> {
    const item = await this.repository.findByProductId(productId);

    if (!item || !item.hasAvailableStock(quantity)) {
      // Publish stock.insufficient event
      const event = createStockInsufficientEvent(orderId, productId, quantity, item?.quantityAvailable || 0);
      await this.producer.publish(TOPICS.STOCK_INSUFFICIENT, orderId, event);
      console.log(`[Saga] Stock insufficient: productId=${productId}, orderId=${orderId}`);
      return false;
    }

    item.reserveStock(quantity);
    await this.repository.save(item);

    // Publish stock.reserved event
    const event = createStockReservedEvent(orderId, productId, quantity);
    await this.producer.publish(TOPICS.STOCK_RESERVED, orderId, event);
    console.log(`[Saga] Stock reserved: productId=${productId}, qty=${quantity}, orderId=${orderId}`);
    return true;
  }

  async releaseStock(productId: string, quantity: number, orderId: string): Promise<void> {
    const item = await this.repository.findByProductId(productId);
    if (!item) throw new Error(`Inventory not found for product: ${productId}`);

    item.releaseStock(quantity);
    await this.repository.save(item);

    // Publish stock.released event
    const event = createStockReleasedEvent(orderId, productId, quantity);
    await this.producer.publish(TOPICS.STOCK_RELEASED, orderId, event);
    console.log(`[Saga] Stock released: productId=${productId}, qty=${quantity}, orderId=${orderId}`);
  }

  async addStock(productId: string, quantity: number): Promise<InventoryItem> {
    const item = await this.repository.findByProductId(productId);
    if (!item) throw new Error(`Inventory not found for product: ${productId}`);

    item.addStock(quantity);
    return this.repository.save(item);
  }
}
