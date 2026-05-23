import { pool } from './database';
import { Product, Category, ProductReview, ProductStatus } from '../../domain/models/Product';

export class ProductRepository {
  async create(product: Omit<Product, 'createdAt' | 'updatedAt'>): Promise<Product> {
    const query = `
      INSERT INTO products (id, name, description, price, category_id, image_url, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *
    `;
    const values = [product.id, product.name, product.description, product.price, product.categoryId, product.imageUrl, product.status];
    const result = await pool.query(query, values);
    return this.mapToProduct(result.rows[0]);
  }

  async findById(id: string): Promise<Product | null> {
    const result = await pool.query('SELECT * FROM products WHERE id = $1', [id]);
    return result.rows.length > 0 ? this.mapToProduct(result.rows[0]) : null;
  }

  async update(id: string, fields: Partial<Product>): Promise<Product | null> {
    const setClauses: string[] = [];
    const values: any[] = [];
    let idx = 1;

    if (fields.name) { setClauses.push(`name = $${idx++}`); values.push(fields.name); }
    if (fields.description !== undefined) { setClauses.push(`description = $${idx++}`); values.push(fields.description); }
    if (fields.price !== undefined) { setClauses.push(`price = $${idx++}`); values.push(fields.price); }
    if (fields.categoryId !== undefined) { setClauses.push(`category_id = $${idx++}`); values.push(fields.categoryId); }
    if (fields.imageUrl !== undefined) { setClauses.push(`image_url = $${idx++}`); values.push(fields.imageUrl); }
    if (fields.status) { setClauses.push(`status = $${idx++}`); values.push(fields.status); }

    setClauses.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const query = `UPDATE products SET ${setClauses.join(', ')} WHERE id = $${idx} RETURNING *`;
    const result = await pool.query(query, values);
    return result.rows.length > 0 ? this.mapToProduct(result.rows[0]) : null;
  }

  async delete(id: string): Promise<boolean> {
    const result = await pool.query('DELETE FROM products WHERE id = $1', [id]);
    return (result.rowCount ?? 0) > 0;
  }

  async findAll(offset: number, limit: number): Promise<Product[]> {
    const result = await pool.query(
      'SELECT * FROM products WHERE status = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3',
      ['ACTIVE', limit, offset]
    );
    return result.rows.map(this.mapToProduct);
  }

  async search(query: string): Promise<Product[]> {
    const result = await pool.query(
      'SELECT * FROM products WHERE (name ILIKE $1 OR description ILIKE $1) AND status = $2 LIMIT 50',
      [`%${query}%`, 'ACTIVE']
    );
    return result.rows.map(this.mapToProduct);
  }

  // Category operations
  async createCategory(category: Omit<Category, 'createdAt'>): Promise<Category> {
    const query = `
      INSERT INTO categories (id, name, description, parent_id)
      VALUES ($1, $2, $3, $4)
      RETURNING *
    `;
    const result = await pool.query(query, [category.id, category.name, category.description, category.parentId]);
    return this.mapToCategory(result.rows[0]);
  }

  async findAllCategories(): Promise<Category[]> {
    const result = await pool.query('SELECT * FROM categories ORDER BY name');
    return result.rows.map(this.mapToCategory);
  }

  // Review operations
  async createReview(review: Omit<ProductReview, 'createdAt'>): Promise<ProductReview> {
    const query = `
      INSERT INTO product_reviews (id, product_id, user_id, rating, comment)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING *
    `;
    const result = await pool.query(query, [review.id, review.productId, review.userId, review.rating, review.comment]);
    return this.mapToReview(result.rows[0]);
  }

  async findReviewsByProductId(productId: string): Promise<ProductReview[]> {
    const result = await pool.query(
      'SELECT * FROM product_reviews WHERE product_id = $1 ORDER BY created_at DESC',
      [productId]
    );
    return result.rows.map(this.mapToReview);
  }

  private mapToProduct(row: any): Product {
    return {
      id: row.id,
      name: row.name,
      description: row.description,
      price: parseFloat(row.price),
      categoryId: row.category_id,
      imageUrl: row.image_url,
      status: row.status as ProductStatus,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  private mapToCategory(row: any): Category {
    return {
      id: row.id,
      name: row.name,
      description: row.description,
      parentId: row.parent_id,
      createdAt: row.created_at,
    };
  }

  private mapToReview(row: any): ProductReview {
    return {
      id: row.id,
      productId: row.product_id,
      userId: row.user_id,
      rating: row.rating,
      comment: row.comment,
      createdAt: row.created_at,
    };
  }
}
