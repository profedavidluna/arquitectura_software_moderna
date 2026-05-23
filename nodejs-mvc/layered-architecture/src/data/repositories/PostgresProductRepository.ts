import { Pool } from 'pg';
import { Product } from '../models/Product';

/**
 * @layer Data Layer
 * @description PostgreSQL implementation of the Product repository.
 * In Layered Architecture, this is a concrete class in the data layer
 * that handles persistence using PostgreSQL.
 *
 * It has the same public API as the in-memory ProductRepository,
 * allowing it to be used as a drop-in replacement.
 */
export class PostgresProductRepository {
  constructor(private readonly pool: Pool) {}

  async save(product: Product): Promise<Product> {
    const query = `
      INSERT INTO products (id, name, description, price, category, stock_quantity, sku, active, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
      RETURNING *
    `;
    const values = [
      product.id,
      product.name,
      product.description,
      product.price,
      product.category,
      product.stockQuantity,
      product.sku,
      product.active,
      product.createdAt,
      product.updatedAt,
    ];

    const result = await this.pool.query(query, values);
    return this.mapRowToProduct(result.rows[0]);
  }

  async findById(id: string): Promise<Product | null> {
    const query = 'SELECT * FROM products WHERE id = $1';
    const result = await this.pool.query(query, [id]);

    if (result.rows.length === 0) return null;
    return this.mapRowToProduct(result.rows[0]);
  }

  async findAll(page: number, size: number): Promise<{ products: Product[]; total: number }> {
    const countQuery = 'SELECT COUNT(*) FROM products WHERE active = true';
    const countResult = await this.pool.query(countQuery);
    const total = parseInt(countResult.rows[0].count);

    const offset = page * size;
    const query = 'SELECT * FROM products WHERE active = true ORDER BY created_at DESC LIMIT $1 OFFSET $2';
    const result = await this.pool.query(query, [size, offset]);

    const products = result.rows.map((row) => this.mapRowToProduct(row));
    return { products, total };
  }

  async findBySku(sku: string): Promise<Product | null> {
    const query = 'SELECT * FROM products WHERE sku = $1';
    const result = await this.pool.query(query, [sku]);

    if (result.rows.length === 0) return null;
    return this.mapRowToProduct(result.rows[0]);
  }

  async search(
    query?: string,
    category?: string,
    minPrice?: number,
    maxPrice?: number
  ): Promise<Product[]> {
    let sql = 'SELECT * FROM products WHERE active = true';
    const values: any[] = [];
    let paramIndex = 1;

    if (query) {
      sql += ` AND (LOWER(name) LIKE $${paramIndex} OR LOWER(description) LIKE $${paramIndex})`;
      values.push(`%${query.toLowerCase()}%`);
      paramIndex++;
    }

    if (category) {
      sql += ` AND LOWER(category) = $${paramIndex}`;
      values.push(category.toLowerCase());
      paramIndex++;
    }

    if (minPrice !== undefined) {
      sql += ` AND price >= $${paramIndex}`;
      values.push(minPrice);
      paramIndex++;
    }

    if (maxPrice !== undefined) {
      sql += ` AND price <= $${paramIndex}`;
      values.push(maxPrice);
      paramIndex++;
    }

    sql += ' ORDER BY created_at DESC';

    const result = await this.pool.query(sql, values);
    return result.rows.map((row) => this.mapRowToProduct(row));
  }

  async update(product: Product): Promise<Product> {
    const query = `
      UPDATE products
      SET name = $2, description = $3, price = $4, category = $5,
          stock_quantity = $6, sku = $7, active = $8, updated_at = $9
      WHERE id = $1
      RETURNING *
    `;
    const values = [
      product.id,
      product.name,
      product.description,
      product.price,
      product.category,
      product.stockQuantity,
      product.sku,
      product.active,
      product.updatedAt,
    ];

    const result = await this.pool.query(query, values);
    return this.mapRowToProduct(result.rows[0]);
  }

  async delete(id: string): Promise<void> {
    const query = 'DELETE FROM products WHERE id = $1';
    await this.pool.query(query, [id]);
  }

  private mapRowToProduct(row: any): Product {
    return {
      id: row.id,
      name: row.name,
      description: row.description,
      price: parseFloat(row.price),
      category: row.category,
      stockQuantity: row.stock_quantity,
      sku: row.sku,
      active: row.active,
      createdAt: new Date(row.created_at),
      updatedAt: new Date(row.updated_at),
    };
  }
}
