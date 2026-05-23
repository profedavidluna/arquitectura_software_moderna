import { Cart, CartItem } from '../models/Cart';

export interface CreateCartDTO {
  userId: string;
}

export interface AddItemDTO {
  productId: string;
  quantity: number;
}

export interface ApplyCouponDTO {
  couponCode: string;
}

export interface CartSummary {
  cart: Cart;
  items: CartItem[];
  subtotal: number;
  discount: number;
  total: number;
}

export interface ICartService {
  createCart(dto: CreateCartDTO): Promise<Cart>;
  getCartById(id: string): Promise<CartSummary | null>;
  addItem(cartId: string, dto: AddItemDTO): Promise<CartItem>;
  removeItem(cartId: string, itemId: string): Promise<boolean>;
  updateItemQuantity(cartId: string, itemId: string, quantity: number): Promise<CartItem | null>;
  applyCoupon(cartId: string, dto: ApplyCouponDTO): Promise<Cart>;
  clearCart(cartId: string): Promise<boolean>;
}
