import { Cart, CartItem } from '../../domain/models/Cart';
import { CartSummary } from '../../domain/interfaces/ICartService';

export interface CreateCartRequest { userId: string; }
export interface AddItemRequest { productId: string; quantity: number; }
export interface UpdateQuantityRequest { quantity: number; }
export interface ApplyCouponRequest { couponCode: string; }

export function toCartSummaryResponse(summary: CartSummary) {
  return {
    id: summary.cart.id,
    userId: summary.cart.userId,
    status: summary.cart.status,
    couponCode: summary.cart.couponCode,
    discountPercent: summary.cart.discountPercent,
    items: summary.items.map(item => ({
      id: item.id,
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      subtotal: item.unitPrice * item.quantity,
    })),
    subtotal: summary.subtotal,
    discount: summary.discount,
    total: summary.total,
  };
}
