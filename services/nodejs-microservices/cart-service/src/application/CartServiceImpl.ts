/**
 * Cart Service Implementation
 * 
 * Demonstrates the Circuit Breaker pattern when calling product-service.
 * If product-service is down, the circuit breaker prevents cascading failures
 * by failing fast instead of waiting for timeouts.
 */
import { v4 as uuidv4 } from 'uuid';
import { Cart, CartItem, CartStatus } from '../domain/models/Cart';
import { ICartService, CreateCartDTO, AddItemDTO, ApplyCouponDTO, CartSummary } from '../domain/interfaces/ICartService';
import { CartRepository } from '../infrastructure/persistence/CartRepository';
import { RedisClient } from '../infrastructure/cache/RedisClient';
import { CircuitBreaker } from '../infrastructure/CircuitBreaker';
import { config } from '../config';

// Simple HTTP client for inter-service communication
async function httpGet(url: string): Promise<any> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  return response.json();
}

export class CartServiceImpl implements ICartService {
  private productCircuitBreaker: CircuitBreaker;

  constructor(
    private readonly cartRepository: CartRepository,
    private readonly redisClient: RedisClient
  ) {
    // Circuit breaker for product-service calls
    this.productCircuitBreaker = new CircuitBreaker('product-service', {
      threshold: 5,
      timeout: 30000,
      successThreshold: 2,
    });
  }

  async createCart(dto: CreateCartDTO): Promise<Cart> {
    const cart: Omit<Cart, 'createdAt' | 'updatedAt'> = {
      id: uuidv4(),
      userId: dto.userId,
      status: CartStatus.ACTIVE,
      discountPercent: 0,
    };
    return this.cartRepository.create(cart);
  }

  async getCartById(id: string): Promise<CartSummary | null> {
    // Check cache first
    const cached = await this.redisClient.get<CartSummary>(`cart:${id}`);
    if (cached) return cached;

    const cart = await this.cartRepository.findById(id);
    if (!cart) return null;

    const items = await this.cartRepository.findItemsByCartId(id);
    const subtotal = items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
    const discount = subtotal * (cart.discountPercent / 100);
    const total = subtotal - discount;

    const summary: CartSummary = { cart, items, subtotal, discount, total };

    // Cache the summary
    await this.redisClient.set(`cart:${id}`, summary, 300);
    return summary;
  }

  async addItem(cartId: string, dto: AddItemDTO): Promise<CartItem> {
    const cart = await this.cartRepository.findById(cartId);
    if (!cart) throw new Error('Cart not found');
    if (cart.status !== CartStatus.ACTIVE) throw new Error('Cart is not active');

    // Validate product exists via circuit breaker
    let productData: any = null;
    try {
      productData = await this.productCircuitBreaker.execute(async () => {
        return httpGet(`${config.productServiceUrl}/api/v1/products/${dto.productId}`);
      });
    } catch (error: any) {
      // If circuit breaker is open or product-service is down,
      // we can still add the item with limited info (graceful degradation)
      console.warn(`[CartService] Product validation failed: ${error.message}`);
    }

    const item: Omit<CartItem, 'createdAt'> = {
      id: uuidv4(),
      cartId,
      productId: dto.productId,
      productName: productData?.name || 'Unknown Product',
      quantity: dto.quantity,
      unitPrice: productData?.price || 0,
    };

    const created = await this.cartRepository.addItem(item);

    // Invalidate cart cache
    await this.redisClient.delete(`cart:${cartId}`);

    return created;
  }

  async removeItem(cartId: string, itemId: string): Promise<boolean> {
    const deleted = await this.cartRepository.deleteItem(itemId, cartId);
    if (deleted) await this.redisClient.delete(`cart:${cartId}`);
    return deleted;
  }

  async updateItemQuantity(cartId: string, itemId: string, quantity: number): Promise<CartItem | null> {
    if (quantity <= 0) throw new Error('Quantity must be positive');
    const updated = await this.cartRepository.updateItemQuantity(itemId, quantity);
    if (updated) await this.redisClient.delete(`cart:${cartId}`);
    return updated;
  }

  async applyCoupon(cartId: string, dto: ApplyCouponDTO): Promise<Cart> {
    const cart = await this.cartRepository.findById(cartId);
    if (!cart) throw new Error('Cart not found');

    // Simple coupon validation (in production, call a coupon service)
    const validCoupons: Record<string, number> = {
      'SAVE10': 10,
      'SAVE20': 20,
      'WELCOME': 15,
    };

    const discount = validCoupons[dto.couponCode.toUpperCase()];
    if (!discount) throw new Error('Invalid coupon code');

    const updated = await this.cartRepository.update(cartId, {
      couponCode: dto.couponCode,
      discountPercent: discount,
    });

    await this.redisClient.delete(`cart:${cartId}`);
    return updated!;
  }

  async clearCart(cartId: string): Promise<boolean> {
    await this.cartRepository.deleteAllItems(cartId);
    await this.redisClient.delete(`cart:${cartId}`);
    return true;
  }
}
