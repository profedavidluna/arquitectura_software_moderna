// =============================================================================
// Order Repository
// =============================================================================
// Repository Pattern for order data access.
// Handles both the orders table and order_items table as a single aggregate.
// =============================================================================

import { Pool } from 'pg';
import { v4 as uuidv4 } from 'uuid';
import { Order, OrderItem, CreateOrderData } from '../../domain/model/Order';
import { OrderStatus } from '../../domain/model/OrderStatus';

export class OrderRepository {
  constructor(private readonly pool: Pool) {}

  /**
   * Retrieve all orders with their items.
   */
  async findAll(): Promise<Order[]> {
    const ordersResult = await this.pool.query(
      'SELECT * FROM orders ORDER BY created_at DESC'
    );

    const orders: Order[] = [];
    for (const row of ordersResult.rows) {
      const items = await this.findItemsByOrderId(row.id);
      orders.push(this.mapToOrder(row, items));
    }

    return orders;
  }

  /**
   * Find an order by ID with its items.
   */
  async findById(id: string): Promise<Order | null> {
    const result = await this.pool.query('SELECT * FROM orders WHERE id = $1', [id]);
    if (result.rows.length === 0) return null;

    const items = await this.findItemsByOrderId(id);
    return this.mapToOrder(result.rows[0], items);
  }

  /**
   * Find all orders for a specific user.
   */
  async findByUserId(userId: string): Promise<Order[]> {
    const ordersResult = await this.pool.query(
      'SELECT * FROM orders WHERE user_id = $1 ORDER BY created_at DESC',
      [userId]
    );

    const orders: Order[] = [];
    for (const row of ordersResult.rows) {
      const items = await this.findItemsByOrderId(row.id);
      orders.push(this.mapToOrder(row, items));
    }

    return orders;
  }

  /**
   * Create a new order with items in a transaction.
   * Uses database transaction to ensure atomicity.
   */
  async create(data: CreateOrderData): Promise<Order> {
    const client = await this.pool.connect();

    try {
      await client.query('BEGIN');

      const orderId = uuidv4();
      const totalAmount = data.items.reduce(
        (sum, item) => sum + item.quantity * item.unitPrice,
        0
      );

      // Insert order
      const orderResult = await client.query(
        `INSERT INTO orders (id, user_id, status, total_amount)
         VALUES ($1, $2, $3, $4)
         RETURNING *`,
        [orderId, data.userId, OrderStatus.PENDING, totalAmount]
      );

      // Insert order items
      const items: OrderItem[] = [];
      for (const item of data.items) {
        const itemId = uuidv4();
        const subtotal = item.quantity * item.unitPrice;

        const itemResult = await client.query(
          `INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, subtotal)
           VALUES ($1, $2, $3, $4, $5, $6, $7)
           RETURNING *`,
          [itemId, orderId, item.productId, item.productName, item.quantity, item.unitPrice, subtotal]
        );

        items.push(this.mapToOrderItem(itemResult.rows[0]));
      }

      await client.query('COMMIT');

      return this.mapToOrder(orderResult.rows[0], items);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Update order status.
   */
  async updateStatus(id: string, status: OrderStatus): Promise<Order | null> {
    const result = await this.pool.query(
      `UPDATE orders SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 RETURNING *`,
      [status, id]
    );

    if (result.rows.length === 0) return null;

    const items = await this.findItemsByOrderId(id);
    return this.mapToOrder(result.rows[0], items);
  }

  /**
   * Find order items by order ID.
   */
  private async findItemsByOrderId(orderId: string): Promise<OrderItem[]> {
    const result = await this.pool.query(
      'SELECT * FROM order_items WHERE order_id = $1',
      [orderId]
    );
    return result.rows.map(this.mapToOrderItem);
  }

  private mapToOrder(row: any, items: OrderItem[]): Order {
    return {
      id: row.id,
      userId: row.user_id,
      status: row.status as OrderStatus,
      totalAmount: parseFloat(row.total_amount),
      items,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  private mapToOrderItem(row: any): OrderItem {
    return {
      id: row.id,
      orderId: row.order_id,
      productId: row.product_id,
      productName: row.product_name,
      quantity: row.quantity,
      unitPrice: parseFloat(row.unit_price),
      subtotal: parseFloat(row.subtotal),
    };
  }
}
