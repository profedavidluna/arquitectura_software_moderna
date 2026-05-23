// =============================================================================
// Order Service Implementation - Saga Orchestrator
// =============================================================================
// This service implements the Saga pattern for distributed transactions.
//
// Saga Flow:
// 1. Client calls create() → Order saved as PENDING
// 2. order.created event published → Inventory Service processes it
// 3. Inventory responds with stock.reserved or stock.insufficient
// 4. Order Service updates order status based on response
//
// Compensating Transaction:
// - If order is cancelled after confirmation → order.cancelled event published
// - Inventory Service releases the reserved stock
// =============================================================================

import { Order, CreateOrderData } from '../domain/model/Order';
import { OrderStatus, isValidTransition } from '../domain/model/OrderStatus';
import { IOrderService } from '../domain/service/IOrderService';
import { OrderRepository } from '../infrastructure/persistence/OrderRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { createOrderCreatedEvent, createOrderConfirmedEvent, createOrderCancelledEvent } from '../infrastructure/messaging/events';

export class OrderService implements IOrderService {
  constructor(
    private readonly repository: OrderRepository,
    private readonly kafkaProducer: KafkaProducer
  ) {}

  async findAll(): Promise<Order[]> {
    return this.repository.findAll();
  }

  async findById(id: string): Promise<Order | null> {
    return this.repository.findById(id);
  }

  async findByUserId(userId: string): Promise<Order[]> {
    return this.repository.findByUserId(userId);
  }

  /**
   * Create a new order - Step 1 of the Saga.
   * The order starts in PENDING state until inventory confirms stock availability.
   */
  async create(data: CreateOrderData): Promise<Order> {
    // 1. Persist the order with PENDING status
    const order = await this.repository.create(data);

    // 2. Publish order.created event to trigger inventory check
    try {
      const event = createOrderCreatedEvent(order);
      await this.kafkaProducer.publish('order.created', order.id, event);
      console.log(`[OrderService] Saga started - Published order.created for order: ${order.id}`);
    } catch (error) {
      console.error(`[OrderService] Failed to publish order.created event:`, error);
      // In production, use outbox pattern to guarantee event delivery
    }

    return order;
  }

  /**
   * Confirm an order - Called when Inventory Service reserves stock.
   * This is Step 3 of the Saga (happy path).
   */
  async confirm(id: string): Promise<Order | null> {
    const order = await this.repository.findById(id);
    if (!order) return null;

    if (!isValidTransition(order.status, OrderStatus.CONFIRMED)) {
      console.warn(`[OrderService] Invalid transition: ${order.status} → CONFIRMED for order ${id}`);
      return order;
    }

    const updatedOrder = await this.repository.updateStatus(id, OrderStatus.CONFIRMED);

    // Publish order.confirmed event
    if (updatedOrder) {
      try {
        const event = createOrderConfirmedEvent(updatedOrder);
        await this.kafkaProducer.publish('order.confirmed', id, event);
        console.log(`[OrderService] Saga completed - Order ${id} confirmed`);
      } catch (error) {
        console.error(`[OrderService] Failed to publish order.confirmed event:`, error);
      }
    }

    return updatedOrder;
  }

  /**
   * Cancel an order - Triggers compensating transaction.
   * Publishes order.cancelled so Inventory Service can release reserved stock.
   */
  async cancel(id: string): Promise<Order | null> {
    const order = await this.repository.findById(id);
    if (!order) return null;

    if (!isValidTransition(order.status, OrderStatus.CANCELLED)) {
      console.warn(`[OrderService] Invalid transition: ${order.status} → CANCELLED for order ${id}`);
      return order;
    }

    const updatedOrder = await this.repository.updateStatus(id, OrderStatus.CANCELLED);

    // Publish order.cancelled event (compensating transaction trigger)
    if (updatedOrder) {
      try {
        const event = createOrderCancelledEvent(updatedOrder);
        await this.kafkaProducer.publish('order.cancelled', id, event);
        console.log(`[OrderService] Compensating transaction - Published order.cancelled for order: ${id}`);
      } catch (error) {
        console.error(`[OrderService] Failed to publish order.cancelled event:`, error);
      }
    }

    return updatedOrder;
  }

  /**
   * Mark order as cancelled due to insufficient stock.
   * This is Step 3 of the Saga (failure path).
   */
  async markInsufficientStock(id: string): Promise<Order | null> {
    const order = await this.repository.findById(id);
    if (!order) return null;

    if (!isValidTransition(order.status, OrderStatus.CANCELLED)) {
      console.warn(`[OrderService] Invalid transition: ${order.status} → CANCELLED for order ${id}`);
      return order;
    }

    const updatedOrder = await this.repository.updateStatus(id, OrderStatus.CANCELLED);
    console.log(`[OrderService] Order ${id} cancelled - insufficient stock`);

    return updatedOrder;
  }
}
