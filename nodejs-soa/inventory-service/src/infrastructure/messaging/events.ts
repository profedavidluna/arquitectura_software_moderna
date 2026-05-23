import { v4 as uuidv4 } from 'uuid';

export const TOPICS = {
  ORDER_CREATED: 'order.created',
  ORDER_CANCELLED: 'order.cancelled',
  STOCK_RESERVED: 'stock.reserved',
  STOCK_INSUFFICIENT: 'stock.insufficient',
  STOCK_RELEASED: 'stock.released',
};

export function createStockReservedEvent(orderId: string, productId: string, quantity: number) {
  return {
    eventId: uuidv4(),
    eventType: 'STOCK_RESERVED',
    timestamp: new Date().toISOString(),
    orderId,
    productId,
    quantity,
  };
}

export function createStockInsufficientEvent(orderId: string, productId: string, requested: number, available: number) {
  return {
    eventId: uuidv4(),
    eventType: 'STOCK_INSUFFICIENT',
    timestamp: new Date().toISOString(),
    orderId,
    productId,
    requestedQuantity: requested,
    availableQuantity: available,
  };
}

export function createStockReleasedEvent(orderId: string, productId: string, quantity: number) {
  return {
    eventId: uuidv4(),
    eventType: 'STOCK_RELEASED',
    timestamp: new Date().toISOString(),
    orderId,
    productId,
    releasedQuantity: quantity,
  };
}
