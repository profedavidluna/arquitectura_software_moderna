import { v4 as uuidv4 } from 'uuid';
import { Product } from '../../data/models/Product';
import { ConflictError, InsufficientStockError, NotFoundError, ValidationError } from '../errors';

/**
 * @layer Business Layer
 * @description Service containing all business logic for Product operations.
 * In Layered Architecture, the business layer sits between Presentation and Data.
 *
 * Dependencies flow DOWNWARD:
 *   Presentation → Business → Data
 *
 * Key characteristics of Layered Architecture:
 * - Direct dependency on the concrete repository (no interface/port)
 * - Uses the same data model defined in the Data layer
 * - Simpler than Hexagonal but more coupled
 * - Business rules are enforced here
 */

/**
 * Repository contract - defines the methods the service needs from any repository.
 * Both ProductRepository (in-memory) and PostgresProductRepository implement this shape.
 */
export interface IProductRepository {
  save(product: Product): Promise<Product>;
  findById(id: string): Promise<Product | null>;
  findAll(page: number, size: number): Promise<{ products: Product[]; total: number }>;
  findBySku(sku: string): Promise<Product | null>;
  search(query?: string, category?: string, minPrice?: number, maxPrice?: number): Promise<Product[]>;
  update(product: Product): Promise<Product>;
  delete(id: string): Promise<void>;
}

export interface CreateProductInput {
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
}

export interface UpdateProductInput {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  stockQuantity?: number;
  sku?: string;
}

export interface PaginatedResult {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export class ProductService {
  constructor(private readonly repository: IProductRepository) {}

  async createProduct(input: CreateProductInput): Promise<Product> {
    // Business rule: price must be > 0
    if (input.price <= 0) {
      throw new ValidationError('Price must be greater than 0');
    }

    // Business rule: stock cannot be negative
    if (input.stockQuantity < 0) {
      throw new ValidationError('Stock quantity cannot be negative');
    }

    // Business rule: SKU must be unique
    const existing = await this.repository.findBySku(input.sku);
    if (existing) {
      throw new ConflictError(`Product with SKU '${input.sku}' already exists`);
    }

    const now = new Date();
    const product: Product = {
      id: uuidv4(),
      name: input.name,
      description: input.description,
      price: input.price,
      category: input.category,
      stockQuantity: input.stockQuantity,
      sku: input.sku,
      active: true,
      createdAt: now,
      updatedAt: now,
    };

    return this.repository.save(product);
  }

  async getProductById(id: string): Promise<Product> {
    const product = await this.repository.findById(id);
    if (!product) {
      throw new NotFoundError(`Product with id '${id}' not found`);
    }
    return product;
  }

  async listProducts(page: number, size: number): Promise<PaginatedResult> {
    // Business rule: max page size is 100
    const effectiveSize = Math.min(size, 100);
    const effectivePage = Math.max(page, 0);

    const { products, total } = await this.repository.findAll(effectivePage, effectiveSize);

    return {
      content: products,
      page: effectivePage,
      size: effectiveSize,
      totalElements: total,
      totalPages: Math.ceil(total / effectiveSize),
    };
  }

  async searchProducts(
    query?: string,
    category?: string,
    minPrice?: number,
    maxPrice?: number
  ): Promise<Product[]> {
    return this.repository.search(query, category, minPrice, maxPrice);
  }

  async updateProduct(id: string, input: UpdateProductInput): Promise<Product> {
    const existing = await this.getProductById(id);

    if (input.price !== undefined && input.price <= 0) {
      throw new ValidationError('Price must be greater than 0');
    }

    if (input.stockQuantity !== undefined && input.stockQuantity < 0) {
      throw new ValidationError('Stock quantity cannot be negative');
    }

    // Check SKU uniqueness if changing
    if (input.sku && input.sku !== existing.sku) {
      const existingBySku = await this.repository.findBySku(input.sku);
      if (existingBySku) {
        throw new ConflictError(`Product with SKU '${input.sku}' already exists`);
      }
    }

    const updated: Product = {
      ...existing,
      name: input.name ?? existing.name,
      description: input.description ?? existing.description,
      price: input.price ?? existing.price,
      category: input.category ?? existing.category,
      stockQuantity: input.stockQuantity ?? existing.stockQuantity,
      sku: input.sku ?? existing.sku,
      updatedAt: new Date(),
    };

    return this.repository.update(updated);
  }

  async deleteProduct(id: string): Promise<void> {
    const product = await this.getProductById(id);
    // Soft delete: set active = false
    const deactivated: Product = {
      ...product,
      active: false,
      updatedAt: new Date(),
    };
    await this.repository.update(deactivated);
  }

  async decreaseStock(id: string, quantity: number): Promise<Product> {
    if (quantity <= 0) {
      throw new ValidationError('Quantity must be greater than 0');
    }

    const product = await this.getProductById(id);
    const newStock = product.stockQuantity - quantity;

    if (newStock < 0) {
      throw new InsufficientStockError(product.stockQuantity, quantity);
    }

    const updated: Product = {
      ...product,
      stockQuantity: newStock,
      updatedAt: new Date(),
    };

    return this.repository.update(updated);
  }

  async increaseStock(id: string, quantity: number): Promise<Product> {
    if (quantity <= 0) {
      throw new ValidationError('Quantity must be greater than 0');
    }

    const product = await this.getProductById(id);
    const updated: Product = {
      ...product,
      stockQuantity: product.stockQuantity + quantity,
      updatedAt: new Date(),
    };

    return this.repository.update(updated);
  }
}
