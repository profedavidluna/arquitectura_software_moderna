// =============================================================================
// Order Domain Entity
// =============================================================================
// The Order is the aggregate root for the order bounded context.
// It contains order items as part of the aggregate.
// =============================================================================

import { OrderStatus } from './OrderStatus';

/**
 * Order item - represents a line item in an order.
 */
export interface OrderItem {
  id: string;
  orderId: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/**
 * Order domain entity - the aggregate root.
 */
export interface Order {
  id: string;
  userId: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItem[];
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Data required to create a new order.
 */
export interface CreateOrderData {
  userId: string;
  items: CreateOrderItemData[];
}

/**
 * Data for a single order item during creation.
 */
export interface CreateOrderItemData {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}
