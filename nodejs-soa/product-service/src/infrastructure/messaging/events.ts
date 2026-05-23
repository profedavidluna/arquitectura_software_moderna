// =============================================================================
// Event Definitions - Product Service
// =============================================================================
// Events are the messages that flow through the ESB (Kafka).
// Each event has a well-defined structure (schema) that acts as a contract
// between the producer and consumers.
//
// Factory Pattern: Helper functions create properly structured events.
// =============================================================================

import { Product } from '../../domain/model/Product';

/**
 * Event published when a new product is created.
 * Other services (e.g., Inventory) can react to this event.
 */
export interface ProductCreatedEvent {
  eventType: 'PRODUCT_CREATED';
  timestamp: string;
  data: {
    productId: string;
    name: string;
    price: number;
    category: string | null;
    sku: string;
  };
}

/**
 * Factory function to create a ProductCreatedEvent.
 * Using factories ensures consistent event structure across the codebase.
 */
export function createProductCreatedEvent(product: Product): ProductCreatedEvent {
  return {
    eventType: 'PRODUCT_CREATED',
    timestamp: new Date().toISOString(),
    data: {
      productId: product.id,
      name: product.name,
      price: product.price,
      category: product.category,
      sku: product.sku,
    },
  };
}
