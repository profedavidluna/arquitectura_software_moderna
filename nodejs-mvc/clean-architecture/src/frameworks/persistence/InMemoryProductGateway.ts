import { Product } from '../../entities/Product';
import { ProductGateway } from '../../usecases/interfaces/ProductGateway';

/**
 * @layer Frameworks & Drivers (Outermost Layer)
 * @description In-memory implementation of the ProductGateway interface.
 * In Clean Architecture, this is the outermost layer that implements
 * interfaces defined by the inner layers.
 *
 * This could be replaced with PostgreSQL, MongoDB, or any other
 * persistence technology without affecting use cases or entities.
 *
 * The Dependency Rule ensures that this layer depends on inner layers,
 * but inner layers know nothing about this implementation.
 */
export class InMemoryProductGateway implements ProductGateway {
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

  async search(query?: string, category?: string, minPrice?: number, maxPrice?: number): Promise<Product[]> {
    let results = Array.from(this.products.values()).filter((p) => p.active);

    if (query) {
      const lower = query.toLowerCase();
      results = results.filter((p) => p.name.toLowerCase().includes(lower) || p.description.toLowerCase().includes(lower));
    }

    if (category) {
      const lower = category.toLowerCase();
      results = results.filter((p) => p.category.toLowerCase() === lower);
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
