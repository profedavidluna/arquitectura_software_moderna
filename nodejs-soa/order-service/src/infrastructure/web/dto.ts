// =============================================================================
// Data Transfer Objects (DTOs) - Order Service
// =============================================================================

/**
 * Request body for creating an order.
 */
export interface CreateOrderDto {
  userId: string;
  items: CreateOrderItemDto[];
}

export interface CreateOrderItemDto {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

/**
 * Response DTO for an order.
 */
export interface OrderResponseDto {
  id: string;
  userId: string;
  status: string;
  totalAmount: number;
  items: OrderItemResponseDto[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemResponseDto {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/**
 * Validates the create order request body.
 */
export function validateCreateOrderDto(body: any): string | null {
  if (!body.userId || typeof body.userId !== 'string') {
    return 'userId is required and must be a string';
  }
  if (!body.items || !Array.isArray(body.items) || body.items.length === 0) {
    return 'items is required and must be a non-empty array';
  }

  for (let i = 0; i < body.items.length; i++) {
    const item = body.items[i];
    if (!item.productId || typeof item.productId !== 'string') {
      return `items[${i}].productId is required and must be a string`;
    }
    if (!item.productName || typeof item.productName !== 'string') {
      return `items[${i}].productName is required and must be a string`;
    }
    if (!item.quantity || typeof item.quantity !== 'number' || item.quantity <= 0) {
      return `items[${i}].quantity is required and must be a positive number`;
    }
    if (!item.unitPrice || typeof item.unitPrice !== 'number' || item.unitPrice <= 0) {
      return `items[${i}].unitPrice is required and must be a positive number`;
    }
  }

  return null;
}
