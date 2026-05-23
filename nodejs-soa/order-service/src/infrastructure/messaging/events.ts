// =============================================================================
// Event Definitions - Order Service
// =============================================================================
// Defines the events that the Order Service publishes and consumes.
//
// Published Events:
// - order.created: When a new order is placed (triggers Saga)
// - order.confirmed: When stock is reserved and order is confirmed
// - order.cancelled: When order is cancelled (triggers compensation)
//
// Consumed Events (from Inventory Service):
// - stock.reserved: Stock was successfully reserved
// - stock.insufficient: Not enough stock available
// =============================================================================

import { Order } from '../../domain/model/Order';

// --- Published Events ---

export interface OrderCreatedEvent {
  eventType: 'ORDER_CREATED';
  timestamp: string;
  data: {
    orderId: string;
    userId: string;
    items: Array<{
      productId: string;
      productName: string;
      quantity: number;
      unitPrice: number;
    }>;
    totalAmount: number;
  };
}

export interface OrderConfirmedEvent {
  eventType: 'ORDER_CONFIRMED';
  timestamp: string;
  data: {
    orderId: string;
    userId: string;
    totalAmount: number;
  };
}

export interface OrderCancelledEvent {
  eventType: 'ORDER_CANCELLED';
  timestamp: string;
  data: {
    orderId: string;
    userId: string;
    items: Array<{
      productId: string;
      quantity: number;
    }>;
  };
}

// --- Consumed Events ---

export interface StockReservedEvent {
  eventType: 'STOCK_RESERVED';
  timestamp: string;
  data: {
    orderId: string;
    items: Array<{
      productId: string;
      quantityReserved: number;
    }>;
  };
}

export interface StockInsufficientEvent {
  eventType: 'STOCK_INSUFFICIENT';
  timestamp: string;
  data: {
    orderId: string;
    productId: string;
    requestedQuantity: number;
    availableQuantity: number;
  };
}

// --- Factory Functions ---

export function createOrderCreatedEvent(order: Order): OrderCreatedEvent {
  return {
    eventType: 'ORDER_CREATED',
    timestamp: new Date().toISOString(),
    data: {
      orderId: order.id,
      userId: order.userId,
      items: order.items.map((item) => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
      })),
      totalAmount: order.totalAmount,
    },
  };
}

export function createOrderConfirmedEvent(order: Order): OrderConfirmedEvent {
  return {
    eventType: 'ORDER_CONFIRMED',
    timestamp: new Date().toISOString(),
    data: {
      orderId: order.id,
      userId: order.userId,
      totalAmount: order.totalAmount,
    },
  };
}

export function createOrderCancelledEvent(order: Order): OrderCancelledEvent {
  return {
    eventType: 'ORDER_CANCELLED',
    timestamp: new Date().toISOString(),
    data: {
      orderId: order.id,
      userId: order.userId,
      items: order.items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
      })),
    },
  };
}
