import { pool } from './database';
import { Order, OrderItem, OrderStatus, SagaState, SagaStep, SagaStatus } from '../../domain/models/Order';

export class OrderRepository {
  async create(order: Omit<Order, 'createdAt' | 'updatedAt'>): Promise<Order> {
    const query = `
      INSERT INTO orders (id, user_id, status, total_amount, shipping_address_id, payment_id, notes)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *
    `;
    const result = await pool.query(query, [
      order.id, order.userId, order.status, order.totalAmount,
      order.shippingAddressId, order.paymentId, order.notes
    ]);
    return this.mapToOrder(result.rows[0]);
  }

  async findById(id: string): Promise<Order | null> {
    const result = await pool.query('SELECT * FROM orders WHERE id = $1', [id]);
    return result.rows.length > 0 ? this.mapToOrder(result.rows[0]) : null;
  }

  async findByUserId(userId: string, offset: number, limit: number): Promise<Order[]> {
    const result = await pool.query(
      'SELECT * FROM orders WHERE user_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3',
      [userId, limit, offset]
    );
    return result.rows.map(this.mapToOrder);
  }

  async updateStatus(id: string, status: OrderStatus, paymentId?: string): Promise<Order | null> {
    let query: string;
    let values: any[];

    if (paymentId) {
      query = 'UPDATE orders SET status = $1, payment_id = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3 RETURNING *';
      values = [status, paymentId, id];
    } else {
      query = 'UPDATE orders SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING *';
      values = [status, id];
    }

    const result = await pool.query(query, values);
    return result.rows.length > 0 ? this.mapToOrder(result.rows[0]) : null;
  }

  // Order Items
  async createItems(items: Omit<OrderItem, 'id'>[]): Promise<OrderItem[]> {
    const results: OrderItem[] = [];
    for (const item of items) {
      const query = `
        INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal)
        VALUES ($1, $2, $3, $4, $5, $6, $7)
        RETURNING *
      `;
      const { v4: uuidv4 } = require('uuid');
      const result = await pool.query(query, [
        uuidv4(), item.orderId, item.productId, item.productName,
        item.quantity, item.unitPrice, item.subtotal
      ]);
      results.push(this.mapToOrderItem(result.rows[0]));
    }
    return results;
  }

  async findItemsByOrderId(orderId: string): Promise<OrderItem[]> {
    const result = await pool.query('SELECT * FROM order_items WHERE order_id = $1', [orderId]);
    return result.rows.map(this.mapToOrderItem);
  }

  // Saga State
  async createSagaState(saga: Omit<SagaState, 'createdAt' | 'updatedAt'>): Promise<SagaState> {
    const query = `
      INSERT INTO saga_state (id, order_id, current_step, status, compensation_data)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING *
    `;
    const result = await pool.query(query, [
      saga.id, saga.orderId, saga.currentStep, saga.status,
      saga.compensationData ? JSON.stringify(saga.compensationData) : null
    ]);
    return this.mapToSagaState(result.rows[0]);
  }

  async updateSagaState(id: string, step: SagaStep, status: SagaStatus, compensationData?: any): Promise<void> {
    await pool.query(
      'UPDATE saga_state SET current_step = $1, status = $2, compensation_data = $3, updated_at = CURRENT_TIMESTAMP WHERE id = $4',
      [step, status, compensationData ? JSON.stringify(compensationData) : null, id]
    );
  }

  private mapToOrder(row: any): Order {
    return {
      id: row.id, userId: row.user_id, status: row.status as OrderStatus,
      totalAmount: parseFloat(row.total_amount), shippingAddressId: row.shipping_address_id,
      paymentId: row.payment_id, notes: row.notes,
      createdAt: row.created_at, updatedAt: row.updated_at,
    };
  }

  private mapToOrderItem(row: any): OrderItem {
    return {
      id: row.id, orderId: row.order_id, productId: row.product_id,
      productName: row.product_name, quantity: row.quantity,
      unitPrice: parseFloat(row.unit_price), subtotal: parseFloat(row.subtotal),
    };
  }

  private mapToSagaState(row: any): SagaState {
    return {
      id: row.id, orderId: row.order_id, currentStep: row.current_step as SagaStep,
      status: row.status as SagaStatus, compensationData: row.compensation_data,
      createdAt: row.created_at, updatedAt: row.updated_at,
    };
  }
}
