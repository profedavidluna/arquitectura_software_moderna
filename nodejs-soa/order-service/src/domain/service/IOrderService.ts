// =============================================================================
// Order Service Interface (Contract)
// =============================================================================
// Defines the contract for order management operations.
// The Order Service acts as the Saga Orchestrator in the distributed transaction.
// =============================================================================

import { Order, CreateOrderData } from '../model/Order';

/**
 * IOrderService - Service contract for order operations.
 * 
 * This service orchestrates the Saga pattern:
 * 1. Creates order in PENDING state
 * 2. Publishes order.created event
 * 3. Waits for inventory response (stock.reserved or stock.insufficient)
 * 4. Updates order status accordingly
 */
export interface IOrderService {
  /**
   * Retrieve all orders.
   */
  findAll(): Promise<Order[]>;

  /**
   * Find a specific order by ID.
   */
  findById(id: string): Promise<Order | null>;

  /**
   * Find all orders for a specific user.
   */
  findByUserId(userId: string): Promise<Order[]>;

  /**
   * Create a new order (starts the Saga).
   * Order is created in PENDING state and order.created event is published.
   */
  create(data: CreateOrderData): Promise<Order>;

  /**
   * Cancel an order (triggers compensating transaction).
   * Publishes order.cancelled event so inventory can release reserved stock.
   */
  cancel(id: string): Promise<Order | null>;

  /**
   * Confirm an order (called when stock is reserved).
   * This is triggered by the stock.reserved event from Inventory Service.
   */
  confirm(id: string): Promise<Order | null>;

  /**
   * Mark order as cancelled due to insufficient stock.
   * This is triggered by the stock.insufficient event from Inventory Service.
   */
  markInsufficientStock(id: string): Promise<Order | null>;
}
