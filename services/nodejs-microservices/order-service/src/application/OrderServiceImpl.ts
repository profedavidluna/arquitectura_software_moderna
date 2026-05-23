/**
 * Order Service Implementation - Saga Orchestrator
 * 
 * Implements the Saga Pattern (Orchestration variant):
 * The order service acts as the central coordinator for the distributed transaction.
 * 
 * Saga Steps:
 * 1. VALIDATE_CART - Get cart details and validate items
 * 2. RESERVE_INVENTORY - Reserve stock for each item
 * 3. PROCESS_PAYMENT - Charge the customer
 * 4. CONFIRM_ORDER - Finalize the order
 * 
 * If any step fails, compensating transactions are executed:
 * - Payment failed → Release inventory reservations
 * - Inventory failed → Cancel order
 * 
 * This ensures data consistency across services without distributed locks.
 */
import { v4 as uuidv4 } from 'uuid';
import { Order, OrderStatus, SagaState, SagaStep, SagaStatus } from '../domain/models/Order';
import { IOrderService, CreateOrderDTO } from '../domain/interfaces/IOrderService';
import { OrderRepository } from '../infrastructure/persistence/OrderRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { ORDER_EVENTS_TOPIC, OrderEventType } from '../infrastructure/messaging/events';
import { config } from '../config';

// Circuit Breaker for external service calls
class CircuitBreaker {
  private state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' = 'CLOSED';
  private failureCount = 0;
  private lastFailureTime = 0;
  private readonly threshold = 5;
  private readonly timeout = 30000;

  constructor(private readonly name: string) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (this.state === 'OPEN') {
      if (Date.now() - this.lastFailureTime > this.timeout) {
        this.state = 'HALF_OPEN';
      } else {
        throw new Error(`Circuit breaker [${this.name}] is OPEN`);
      }
    }
    try {
      const result = await fn();
      this.state = 'CLOSED';
      this.failureCount = 0;
      return result;
    } catch (error) {
      this.failureCount++;
      this.lastFailureTime = Date.now();
      if (this.failureCount >= this.threshold) this.state = 'OPEN';
      throw error;
    }
  }
}

export class OrderServiceImpl implements IOrderService {
  private cartBreaker = new CircuitBreaker('cart-service');
  private inventoryBreaker = new CircuitBreaker('inventory-service');
  private paymentBreaker = new CircuitBreaker('payment-service');

  constructor(
    private readonly orderRepository: OrderRepository,
    private readonly kafkaProducer: KafkaProducer
  ) {}

  async createOrder(dto: CreateOrderDTO): Promise<Order> {
    const orderId = uuidv4();
    const sagaId = uuidv4();

    // Step 1: Validate cart and get items
    let cartData: any;
    try {
      cartData = await this.cartBreaker.execute(async () => {
        const response = await fetch(`${config.cartServiceUrl}/api/v1/carts/${dto.cartId}`);
        if (!response.ok) throw new Error('Cart not found');
        return response.json();
      });
    } catch (error: any) {
      throw new Error(`Failed to validate cart: ${error.message}`);
    }

    if (!cartData.items || cartData.items.length === 0) {
      throw new Error('Cart is empty');
    }

    // Create order in PENDING state
    const order = await this.orderRepository.create({
      id: orderId,
      userId: dto.userId,
      status: OrderStatus.PENDING,
      totalAmount: cartData.total,
      shippingAddressId: dto.shippingAddressId,
      notes: dto.notes,
    });

    // Create order items from cart
    const orderItems = cartData.items.map((item: any) => ({
      orderId,
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      subtotal: item.unitPrice * item.quantity,
    }));
    await this.orderRepository.createItems(orderItems);

    // Create saga state for tracking
    await this.orderRepository.createSagaState({
      id: sagaId,
      orderId,
      currentStep: SagaStep.RESERVE_INVENTORY,
      status: SagaStatus.IN_PROGRESS,
      compensationData: { cartId: dto.cartId, items: cartData.items },
    });

    // Step 2: Reserve inventory (async via Kafka)
    await this.kafkaProducer.publish(ORDER_EVENTS_TOPIC, {
      type: OrderEventType.ORDER_CREATED,
      data: {
        id: orderId,
        orderId,
        userId: dto.userId,
        items: orderItems,
        totalAmount: cartData.total,
        paymentMethod: dto.paymentMethod,
      },
    });

    // Step 3: Request inventory reservation
    try {
      await this.inventoryBreaker.execute(async () => {
        for (const item of cartData.items) {
          const response = await fetch(`${config.inventoryServiceUrl}/api/v1/inventory/${item.productId}/reserve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderId, quantity: item.quantity }),
          });
          if (!response.ok) throw new Error(`Failed to reserve inventory for ${item.productId}`);
        }
      });

      // Update order status
      await this.orderRepository.updateStatus(orderId, OrderStatus.INVENTORY_RESERVED);
      await this.orderRepository.updateSagaState(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.IN_PROGRESS);

    } catch (error: any) {
      // Compensation: mark order as failed
      await this.orderRepository.updateStatus(orderId, OrderStatus.FAILED);
      await this.orderRepository.updateSagaState(sagaId, SagaStep.RESERVE_INVENTORY, SagaStatus.FAILED);
      throw new Error(`Inventory reservation failed: ${error.message}`);
    }

    // Step 4: Process payment
    try {
      const paymentResponse = await this.paymentBreaker.execute(async () => {
        const response = await fetch(`${config.paymentServiceUrl}/api/v1/payments/process`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            orderId,
            userId: dto.userId,
            amount: cartData.total,
            method: dto.paymentMethod,
          }),
        });
        if (!response.ok) throw new Error('Payment processing failed');
        return response.json();
      });

      // Payment successful - confirm order
      await this.orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED, paymentResponse.id);
      await this.orderRepository.updateSagaState(sagaId, SagaStep.COMPLETED, SagaStatus.COMPLETED);

      await this.kafkaProducer.publish(ORDER_EVENTS_TOPIC, {
        type: OrderEventType.ORDER_CONFIRMED,
        data: { id: orderId, orderId, userId: dto.userId, totalAmount: cartData.total },
      });

    } catch (error: any) {
      // Compensation: release inventory, mark order failed
      console.log(`[Saga] Payment failed for order ${orderId}, compensating...`);
      await this.compensateInventory(cartData.items, orderId);
      await this.orderRepository.updateStatus(orderId, OrderStatus.FAILED);
      await this.orderRepository.updateSagaState(sagaId, SagaStep.PROCESS_PAYMENT, SagaStatus.FAILED);

      await this.kafkaProducer.publish(ORDER_EVENTS_TOPIC, {
        type: OrderEventType.ORDER_FAILED,
        data: { id: orderId, orderId, reason: error.message },
      });

      throw new Error(`Payment failed: ${error.message}`);
    }

    return (await this.orderRepository.findById(orderId))!;
  }

  async getOrderById(id: string): Promise<Order | null> {
    return this.orderRepository.findById(id);
  }

  async getOrdersByUserId(userId: string, page: number, limit: number): Promise<Order[]> {
    const offset = (page - 1) * limit;
    return this.orderRepository.findByUserId(userId, offset, limit);
  }

  async cancelOrder(id: string): Promise<Order | null> {
    const order = await this.orderRepository.findById(id);
    if (!order) return null;
    if (order.status === OrderStatus.CANCELLED) throw new Error('Order already cancelled');
    if (order.status === OrderStatus.CONFIRMED) throw new Error('Cannot cancel confirmed order, request refund instead');

    const updated = await this.orderRepository.updateStatus(id, OrderStatus.CANCELLED);

    await this.kafkaProducer.publish(ORDER_EVENTS_TOPIC, {
      type: OrderEventType.ORDER_CANCELLED,
      data: { id, orderId: id, userId: order.userId },
    });

    return updated;
  }

  /**
   * Compensation: Release inventory reservations when payment fails.
   * This is the "undo" step in the Saga pattern.
   */
  private async compensateInventory(items: any[], orderId: string): Promise<void> {
    try {
      for (const item of items) {
        await fetch(`${config.inventoryServiceUrl}/api/v1/inventory/${item.productId}/release`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ orderId, quantity: item.quantity }),
        });
      }
      console.log(`[Saga] Inventory released for order ${orderId}`);
    } catch (error) {
      console.error(`[Saga] Failed to compensate inventory for order ${orderId}:`, error);
      // In production, this would go to a dead letter queue for manual resolution
    }
  }
}
