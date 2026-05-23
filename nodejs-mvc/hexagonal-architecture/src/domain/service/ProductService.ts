import { Product } from '../model/Product';
import { ProductRepositoryPort } from '../port/output/ProductRepositoryPort';
import {
  CreateProductCommand,
  PaginatedResult,
  ProductServicePort,
  SearchCriteria,
  UpdateProductCommand,
} from '../port/input/ProductServicePort';
import { v4 as uuidv4 } from 'uuid';

/**
 * @layer Domain Service
 * @description Implements the input port (ProductServicePort) and orchestrates
 * domain logic. This is the APPLICATION SERVICE in Hexagonal Architecture.
 *
 * It depends on:
 * - The domain model (Product)
 * - The output port interface (ProductRepositoryPort)
 *
 * It does NOT depend on:
 * - Express or any web framework
 * - Any specific database or persistence technology
 * - Any external infrastructure
 */
export class ProductService implements ProductServicePort {
  constructor(private readonly repository: ProductRepositoryPort) {}

  async createProduct(command: CreateProductCommand): Promise<Product> {
    // Business rule: price must be > 0
    Product.validatePrice(command.price);

    // Business rule: stock cannot be negative
    Product.validateStock(command.stockQuantity);

    // Business rule: SKU must be unique
    const existingProduct = await this.repository.findBySku(command.sku);
    if (existingProduct) {
      throw new Error(`Product with SKU '${command.sku}' already exists`);
    }

    const now = new Date();
    const product = new Product({
      id: uuidv4(),
      name: command.name,
      description: command.description,
      price: command.price,
      category: command.category,
      stockQuantity: command.stockQuantity,
      sku: command.sku,
      active: true,
      createdAt: now,
      updatedAt: now,
    });

    return this.repository.save(product);
  }

  async getProductById(id: string): Promise<Product> {
    const product = await this.repository.findById(id);
    if (!product) {
      throw new Error(`Product with id '${id}' not found`);
    }
    return product;
  }

  async listProducts(page: number, size: number): Promise<PaginatedResult<Product>> {
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

  async searchProducts(criteria: SearchCriteria): Promise<Product[]> {
    return this.repository.search(
      criteria.query,
      criteria.category,
      criteria.minPrice,
      criteria.maxPrice
    );
  }

  async updateProduct(id: string, command: UpdateProductCommand): Promise<Product> {
    const existing = await this.getProductById(id);

    if (command.price !== undefined) {
      Product.validatePrice(command.price);
    }

    if (command.stockQuantity !== undefined) {
      Product.validateStock(command.stockQuantity);
    }

    // Check SKU uniqueness if changing SKU
    if (command.sku && command.sku !== existing.sku) {
      const existingBySku = await this.repository.findBySku(command.sku);
      if (existingBySku) {
        throw new Error(`Product with SKU '${command.sku}' already exists`);
      }
    }

    const updatedProduct = new Product({
      id: existing.id,
      name: command.name ?? existing.name,
      description: command.description ?? existing.description,
      price: command.price ?? existing.price,
      category: command.category ?? existing.category,
      stockQuantity: command.stockQuantity ?? existing.stockQuantity,
      sku: command.sku ?? existing.sku,
      active: existing.active,
      createdAt: existing.createdAt,
      updatedAt: new Date(),
    });

    return this.repository.update(updatedProduct);
  }

  async deleteProduct(id: string): Promise<void> {
    const product = await this.getProductById(id);
    const deactivated = product.deactivate();
    await this.repository.update(deactivated);
  }

  async decreaseStock(id: string, quantity: number): Promise<Product> {
    const product = await this.getProductById(id);
    const updated = product.decreaseStock(quantity);
    return this.repository.update(updated);
  }

  async increaseStock(id: string, quantity: number): Promise<Product> {
    const product = await this.getProductById(id);
    const updated = product.increaseStock(quantity);
    return this.repository.update(updated);
  }
}
