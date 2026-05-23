// =============================================================================
// Product Repository
// =============================================================================
// Repository Pattern: Abstracts data access logic from business logic.
// The service layer doesn't know (or care) how data is stored.
// This could be swapped for MongoDB, DynamoDB, etc. without changing the service.
// =============================================================================

import { Pool } from 'pg';
import { v4 as uuidv4 } from 'uuid';
import { Product, CreateProductData, UpdateProductData } from '../../domain/model/Product';

export class ProductRepository {
  constructor(private readonly pool: Pool) {}

  /**
   * Retrieve all active products.
   */
  async findAll(): Promise<Product[]> {
    const result = await this.pool.query(
      'SELECT * FROM products WHERE active = true ORDER BY created_at DESC'
    );
    return result.rows.map(this.mapToProduct);
  }

  /**
   * Find a product by its UUID.
   */
  async findById(id: string): Promise<Product | null> {
    const result = await this.pool.query(
      'SELECT * FROM products WHERE id = $1',
      [id]
    );
    return result.rows.length > 0 ? this.mapToProduct(result.rows[0]) : null;
  }

  /**
   * Create a new product with a generated UUID.
   */
  async create(data: CreateProductData): Promise<Product> {
    const id = uuidv4();
    const result = await this.pool.query(
      `INSERT INTO products (id, name, description, price, category, sku)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [id, data.name, data.description || null, data.price, data.category || null, data.sku]
    );
    return this.mapToProduct(result.rows[0]);
  }

  /**
   * Update an existing product. Only updates provided fields.
   */
  async update(id: string, data: UpdateProductData): Promise<Product | null> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (data.name !== undefined) {
      fields.push(`name = $${paramIndex++}`);
      values.push(data.name);
    }
    if (data.description !== undefined) {
      fields.push(`description = $${paramIndex++}`);
      values.push(data.description);
    }
    if (data.price !== undefined) {
      fields.push(`price = $${paramIndex++}`);
      values.push(data.price);
    }
    if (data.category !== undefined) {
      fields.push(`category = $${paramIndex++}`);
      values.push(data.category);
    }
    if (data.active !== undefined) {
      fields.push(`active = $${paramIndex++}`);
      values.push(data.active);
    }

    if (fields.length === 0) return this.findById(id);

    fields.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(id);

    const result = await this.pool.query(
      `UPDATE products SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows.length > 0 ? this.mapToProduct(result.rows[0]) : null;
  }

  /**
   * Soft delete - marks product as inactive rather than removing data.
   */
  async delete(id: string): Promise<boolean> {
    const result = await this.pool.query(
      'UPDATE products SET active = false, updated_at = CURRENT_TIMESTAMP WHERE id = $1',
      [id]
    );
    return (result.rowCount ?? 0) > 0;
  }

  /**
   * Maps a database row to a Product domain entity.
   * This separation keeps the domain model independent of the persistence layer.
   */
  private mapToProduct(row: any): Product {
    return {
      id: row.id,
      name: row.name,
      description: row.description,
      price: parseFloat(row.price),
      category: row.category,
      sku: row.sku,
      active: row.active,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }
}
