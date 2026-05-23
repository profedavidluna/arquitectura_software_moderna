import { pool } from './database';
import { Cart, CartItem, CartStatus } from '../../domain/models/Cart';

export class CartRepository {
  async create(cart: Omit<Cart, 'createdAt' | 'updatedAt'>): Promise<Cart> {
    const query = `
      INSERT INTO carts (id, user_id, status, coupon_code, discount_percent)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING *
    `;
    const result = await pool.query(query, [cart.id, cart.userId, cart.status, cart.couponCode, cart.discountPercent]);
    return this.mapToCart(result.rows[0]);
  }

  async findById(id: string): Promise<Cart | null> {
    const result = await pool.query('SELECT * FROM carts WHERE id = $1', [id]);
    return result.rows.length > 0 ? this.mapToCart(result.rows[0]) : null;
  }

  async update(id: string, fields: Partial<Cart>): Promise<Cart | null> {
    const setClauses: string[] = [];
    const values: any[] = [];
    let idx = 1;

    if (fields.status) { setClauses.push(`status = $${idx++}`); values.push(fields.status); }
    if (fields.couponCode !== undefined) { setClauses.push(`coupon_code = $${idx++}`); values.push(fields.couponCode); }
    if (fields.discountPercent !== undefined) { setClauses.push(`discount_percent = $${idx++}`); values.push(fields.discountPercent); }

    setClauses.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const query = `UPDATE carts SET ${setClauses.join(', ')} WHERE id = $${idx} RETURNING *`;
    const result = await pool.query(query, values);
    return result.rows.length > 0 ? this.mapToCart(result.rows[0]) : null;
  }

  // Cart Items
  async addItem(item: Omit<CartItem, 'createdAt'>): Promise<CartItem> {
    const query = `
      INSERT INTO cart_items (id, cart_id, product_id, product_name, quantity, unit_price)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `;
    const result = await pool.query(query, [item.id, item.cartId, item.productId, item.productName, item.quantity, item.unitPrice]);
    return this.mapToCartItem(result.rows[0]);
  }

  async findItemsByCartId(cartId: string): Promise<CartItem[]> {
    const result = await pool.query('SELECT * FROM cart_items WHERE cart_id = $1', [cartId]);
    return result.rows.map(this.mapToCartItem);
  }

  async findItemById(itemId: string): Promise<CartItem | null> {
    const result = await pool.query('SELECT * FROM cart_items WHERE id = $1', [itemId]);
    return result.rows.length > 0 ? this.mapToCartItem(result.rows[0]) : null;
  }

  async updateItemQuantity(itemId: string, quantity: number): Promise<CartItem | null> {
    const result = await pool.query(
      'UPDATE cart_items SET quantity = $1 WHERE id = $2 RETURNING *',
      [quantity, itemId]
    );
    return result.rows.length > 0 ? this.mapToCartItem(result.rows[0]) : null;
  }

  async deleteItem(itemId: string, cartId: string): Promise<boolean> {
    const result = await pool.query('DELETE FROM cart_items WHERE id = $1 AND cart_id = $2', [itemId, cartId]);
    return (result.rowCount ?? 0) > 0;
  }

  async deleteAllItems(cartId: string): Promise<void> {
    await pool.query('DELETE FROM cart_items WHERE cart_id = $1', [cartId]);
  }

  private mapToCart(row: any): Cart {
    return {
      id: row.id,
      userId: row.user_id,
      status: row.status as CartStatus,
      couponCode: row.coupon_code,
      discountPercent: parseFloat(row.discount_percent) || 0,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  private mapToCartItem(row: any): CartItem {
    return {
      id: row.id,
      cartId: row.cart_id,
      productId: row.product_id,
      productName: row.product_name,
      quantity: row.quantity,
      unitPrice: parseFloat(row.unit_price),
      createdAt: row.created_at,
    };
  }
}
