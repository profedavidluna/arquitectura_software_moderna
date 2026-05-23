import { Product } from '../../../domain/model/Product';
import { ProductRepositoryPort } from '../../../domain/port/output/ProductRepositoryPort';

/**
 * @layer Adapter - Output (Secondary/Driven)
 * @description In-memory implementation of the ProductRepositoryPort.
 * This is a SECONDARY ADAPTER in Hexagonal Architecture.
 *
 * It implements the output port interface defined in the domain layer.
 * The domain service doesn't know about this class - it only knows
 * about the ProductRepositoryPort interface.
 *
 * This could be replaced with a PostgreSQL, MongoDB, or any other
 * persistence adapter without changing the domain logic.
 */
export class InMemoryProductRepository implements ProductRepositoryPort {
  private products: Map<string, Product> = new Map();

  async save(product: Product): Promise<Product> {
    this.products.set(product.id, product);
    return product;
  }

  async findById(id: string): Promise<Product | null> {
    return this.products.get(id) || null;
  }

  async findAll(page: number, size: number): Promise<{ products: Product[]; total: number }> {
    const allActive = Array.from(this.products.values()).filter((p) => p.active);
    const total = allActive.length;
    const start = page * size;
    const products = allActive.slice(start, start + size);
    return { products, total };
  }

  async findBySku(sku: string): Promise<Product | null> {
    const found = Array.from(this.products.values()).find((p) => p.sku === sku);
    return found || null;
  }

  async search(
    query?: string,
    category?: string,
    minPrice?: number,
    maxPrice?: number
  ): Promise<Product[]> {
    let results = Array.from(this.products.values()).filter((p) => p.active);

    if (query) {
      const lowerQuery = query.toLowerCase();
      results = results.filter(
        (p) =>
          p.name.toLowerCase().includes(lowerQuery) ||
          p.description.toLowerCase().includes(lowerQuery)
      );
    }

    if (category) {
      const lowerCategory = category.toLowerCase();
      results = results.filter((p) => p.category.toLowerCase() === lowerCategory);
    }

    if (minPrice !== undefined) {
      results = results.filter((p) => p.price >= minPrice);
    }

    if (maxPrice !== undefined) {
      results = results.filter((p) => p.price <= maxPrice);
    }

    return results;
  }

  async update(product: Product): Promise<Product> {
    this.products.set(product.id, product);
    return product;
  }

  async delete(id: string): Promise<void> {
    this.products.delete(id);
  }
}
