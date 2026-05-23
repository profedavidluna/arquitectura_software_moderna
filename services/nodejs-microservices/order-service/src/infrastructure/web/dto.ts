import { Order } from '../../domain/models/Order';

export interface CreateOrderRequest {
  userId: string;
  cartId: string;
  shippingAddressId?: string;
  notes?: string;
  paymentMethod: string;
}

export function toOrderResponse(order: Order) {
  return {
    id: order.id,
    userId: order.userId,
    status: order.status,
    totalAmount: order.totalAmount,
    shippingAddressId: order.shippingAddressId,
    paymentId: order.paymentId,
    notes: order.notes,
    createdAt: order.createdAt.toISOString(),
    updatedAt: order.updatedAt.toISOString(),
  };
}
