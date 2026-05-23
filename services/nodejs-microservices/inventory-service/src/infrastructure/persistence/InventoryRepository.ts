import { pool } from './database';
import { InventoryItem, Reservation, ReservationStatus } from '../../domain/models/Inventory';

export class InventoryRepository {
  async create(item: Omit<InventoryItem, 'updatedAt'>): Promise<InventoryItem> {
    const query = `
      INSERT INTO inventory (id, product_id, quantity, reserved_quantity, reorder_level)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING *
    `;
    const result = await pool.query(query, [item.id, item.productId, item.quantity, item.reservedQuantity, item.reorderLevel]);
    return this.mapToInventory(result.rows[0]);
  }

  async findByProductId(productId: string): Promise<InventoryItem | null> {
    const result = await pool.query('SELECT * FROM inventory WHERE product_id = $1', [productId]);
    return result.rows.length > 0 ? this.mapToInventory(result.rows[0]) : null;
  }

  async updateQuantity(productId: string, quantity: number): Promise<InventoryItem | null> {
    const result = await pool.query(
      'UPDATE inventory SET quantity = $1, updated_at = CURRENT_TIMESTAMP WHERE product_id = $2 RETURNING *',
      [quantity, productId]
    );
    return result.rows.length > 0 ? this.mapToInventory(result.rows[0]) : null;
  }

  async reserveStock(productId: string, quantity: number): Promise<InventoryItem | null> {
    // Atomic operation: decrease available, increase reserved
    const result = await pool.query(
      `UPDATE inventory SET quantity = quantity - $1, reserved_quantity = reserved_quantity + $1, 
       updated_at = CURRENT_TIMESTAMP WHERE product_id = $2 AND quantity >= $1 RETURNING *`,
      [quantity, productId]
    );
    return result.rows.length > 0 ? this.mapToInventory(result.rows[0]) : null;
  }

  async releaseStock(productId: string, quantity: number): Promise<InventoryItem | null> {
    const result = await pool.query(
      `UPDATE inventory SET quantity = quantity + $1, reserved_quantity = reserved_quantity - $1,
       updated_at = CURRENT_TIMESTAMP WHERE product_id = $2 RETURNING *`,
      [quantity, productId]
    );
    return result.rows.length > 0 ? this.mapToInventory(result.rows[0]) : null;
  }

  async confirmReservation(productId: string, quantity: number): Promise<InventoryItem | null> {
    // Decrease reserved (stock already removed from available)
    const result = await pool.query(
      `UPDATE inventory SET reserved_quantity = reserved_quantity - $1,
       updated_at = CURRENT_TIMESTAMP WHERE product_id = $2 RETURNING *`,
      [quantity, productId]
    );
    return result.rows.length > 0 ? this.mapToInventory(result.rows[0]) : null;
  }

  async findAll(): Promise<InventoryItem[]> {
    const result = await pool.query('SELECT * FROM inventory ORDER BY product_id');
    return result.rows.map(this.mapToInventory);
  }

  // Reservation tracking
  async createReservation(reservation: Omit<Reservation, 'createdAt'>): Promise<Reservation> {
    const query = `
      INSERT INTO reservations (id, order_id, product_id, quantity, status, expires_at)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `;
    const result = await pool.query(query, [
      reservation.id, reservation.orderId, reservation.productId,
      reservation.quantity, reservation.status, reservation.expiresAt,
    ]);
    return this.mapToReservation(result.rows[0]);
  }

  async findReservationsByOrderId(orderId: string): Promise<Reservation[]> {
    const result = await pool.query(
      'SELECT * FROM reservations WHERE order_id = $1 AND status = $2',
      [orderId, ReservationStatus.ACTIVE]
    );
    return result.rows.map(this.mapToReservation);
  }

  async updateReservationStatus(orderId: string, productId: string, status: ReservationStatus): Promise<void> {
    await pool.query(
      'UPDATE reservations SET status = $1 WHERE order_id = $2 AND product_id = $3',
      [status, orderId, productId]
    );
  }

  async updateReservationStatusByOrder(orderId: string, status: ReservationStatus): Promise<void> {
    await pool.query('UPDATE reservations SET status = $1 WHERE order_id = $2', [status, orderId]);
  }

  private mapToInventory(row: any): InventoryItem {
    return {
      id: row.id, productId: row.product_id, quantity: row.quantity,
      reservedQuantity: row.reserved_quantity, reorderLevel: row.reorder_level,
      updatedAt: row.updated_at,
    };
  }

  private mapToReservation(row: any): Reservation {
    return {
      id: row.id, orderId: row.order_id, productId: row.product_id,
      quantity: row.quantity, status: row.status as ReservationStatus,
      expiresAt: row.expires_at, createdAt: row.created_at,
    };
  }
}
