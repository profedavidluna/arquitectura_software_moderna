/**
 * Inventory Service Implementation
 * 
 * Manages stock levels with reservation support.
 * Uses optimistic locking (WHERE quantity >= requested) to prevent overselling.
 * 
 * Stock States:
 * - Available: quantity (can be sold)
 * - Reserved: reserved_quantity (held for pending orders)
 * - Total: quantity + reserved_quantity
 * 
 * Flow:
 * 1. Order created → Reserve stock (move from available to reserved)
 * 2. Payment confirmed → Confirm reservation (decrease reserved)
 * 3. Order cancelled → Release stock (move from reserved back to available)
 */
import { v4 as uuidv4 } from 'uuid';
import { InventoryItem, Reservation, ReservationStatus } from '../domain/models/Inventory';
import { IInventoryService, CreateInventoryDTO, ReserveStockDTO } from '../domain/interfaces/IInventoryService';
import { InventoryRepository } from '../infrastructure/persistence/InventoryRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { INVENTORY_EVENTS_TOPIC, InventoryEventType } from '../infrastructure/messaging/events';

export class InventoryServiceImpl implements IInventoryService {
  constructor(
    private readonly inventoryRepository: InventoryRepository,
    private readonly kafkaProducer: KafkaProducer
  ) {}

  async createInventory(dto: CreateInventoryDTO): Promise<InventoryItem> {
    const item: Omit<InventoryItem, 'updatedAt'> = {
      id: uuidv4(),
      productId: dto.productId,
      quantity: dto.quantity,
      reservedQuantity: 0,
      reorderLevel: dto.reorderLevel || 10,
    };
    return this.inventoryRepository.create(item);
  }

  async getByProductId(productId: string): Promise<InventoryItem | null> {
    return this.inventoryRepository.findByProductId(productId);
  }

  async updateStock(productId: string, quantity: number): Promise<InventoryItem | null> {
    return this.inventoryRepository.updateQuantity(productId, quantity);
  }

  async reserveStock(productId: string, dto: ReserveStockDTO): Promise<Reservation> {
    // Attempt atomic reservation (fails if insufficient stock)
    const updated = await this.inventoryRepository.reserveStock(productId, dto.quantity);

    if (!updated) {
      // Insufficient stock - publish failure event
      await this.kafkaProducer.publish(INVENTORY_EVENTS_TOPIC, {
        type: InventoryEventType.INVENTORY_RESERVATION_FAILED,
        data: { orderId: dto.orderId, productId, requestedQuantity: dto.quantity },
      });
      throw new Error(`Insufficient stock for product ${productId}`);
    }

    // Create reservation record
    const reservation = await this.inventoryRepository.createReservation({
      id: uuidv4(),
      orderId: dto.orderId,
      productId,
      quantity: dto.quantity,
      status: ReservationStatus.ACTIVE,
      expiresAt: new Date(Date.now() + 15 * 60 * 1000), // 15 min expiry
    });

    // Publish success event
    await this.kafkaProducer.publish(INVENTORY_EVENTS_TOPIC, {
      type: InventoryEventType.INVENTORY_RESERVED,
      data: { orderId: dto.orderId, productId, quantity: dto.quantity },
    });

    // Check if stock is low
    if (updated.quantity <= updated.reorderLevel) {
      await this.kafkaProducer.publish(INVENTORY_EVENTS_TOPIC, {
        type: InventoryEventType.LOW_STOCK_ALERT,
        data: { productId, currentQuantity: updated.quantity, reorderLevel: updated.reorderLevel },
      });
    }

    return reservation;
  }

  async releaseStock(productId: string, orderId: string): Promise<boolean> {
    const reservations = await this.inventoryRepository.findReservationsByOrderId(orderId);
    const reservation = reservations.find(r => r.productId === productId);

    if (!reservation) return false;

    await this.inventoryRepository.releaseStock(productId, reservation.quantity);
    await this.inventoryRepository.updateReservationStatus(orderId, productId, ReservationStatus.RELEASED);

    await this.kafkaProducer.publish(INVENTORY_EVENTS_TOPIC, {
      type: InventoryEventType.INVENTORY_RELEASED,
      data: { orderId, productId, quantity: reservation.quantity },
    });

    return true;
  }

  async confirmReservation(orderId: string): Promise<boolean> {
    const reservations = await this.inventoryRepository.findReservationsByOrderId(orderId);
    if (reservations.length === 0) return false;

    for (const reservation of reservations) {
      await this.inventoryRepository.confirmReservation(reservation.productId, reservation.quantity);
    }

    await this.inventoryRepository.updateReservationStatusByOrder(orderId, ReservationStatus.CONFIRMED);
    return true;
  }

  async listAll(): Promise<InventoryItem[]> {
    return this.inventoryRepository.findAll();
  }
}
