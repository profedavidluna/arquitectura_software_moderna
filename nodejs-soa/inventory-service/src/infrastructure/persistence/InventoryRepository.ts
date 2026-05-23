import { Pool } from 'pg';
import { InventoryItem } from '../../domain/model/InventoryItem';
import { v4 as uuidv4 } from 'uuid';

/**
 * Repository Pattern: Abstracts data access for inventory items.
 */
export class InventoryRepository {
  constructor(private readonly pool: Pool) {}

  async save(item: InventoryItem): Promise<InventoryItem> {
    const query = `
      INSERT INTO inventory (id, product_id, product_name, quantity_available, quantity_reserved, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6)
      ON CONFLICT (product_id) DO UPDATE SET
        quantity_available = $4, quantity_reserved = $5, updated_at = $6
      RETURNING *
    `;
    const result = await this.pool.query(query, [
      item.id, item.productId, item.productName,
      item.quantityAvailable, item.quantityReserved, item.updatedAt,
    ]);
    return this.mapRow(result.rows[0]);
  }

  async findByProductId(productId: string): Promise<InventoryItem | null> {
    const result = await this.pool.query('SELECT * FROM inventory WHERE product_id = $1', [productId]);
    return result.rows.length > 0 ? this.mapRow(result.rows[0]) : null;
  }

  async findAll(): Promise<InventoryItem[]> {
    const result = await this.pool.query('SELECT * FROM inventory ORDER BY updated_at DESC');
    return result.rows.map(this.mapRow);
  }

  private mapRow(row: any): InventoryItem {
    return new InventoryItem({
      id: row.id,
      productId: row.product_id,
      productName: row.product_name,
      quantityAvailable: row.quantity_available,
      quantityReserved: row.quantity_reserved,
      updatedAt: new Date(row.updated_at),
    });
  }
}
