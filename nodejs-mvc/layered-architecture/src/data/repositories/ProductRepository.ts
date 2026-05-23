import { Product } from '../models/Product';

/**
 * @layer Data Layer
 * @description Repository responsible for data access operations.
 * In Layered Architecture, the data layer is the bottom layer that handles
 * all persistence concerns.
 *
 * The Business layer directly depends on this concrete class (no interface).
 * This is the key difference from Hexagonal Architecture - tighter coupling
 * but simpler to understand and implement.
 */
export class ProductRepository {
  private products: Map<string, Product> = new Map();

  async save(product: Product): Promise<Product> {
    this.products.set(product.id, { ...product });
    return { ...product };
  }

  async findById(id: string): Promise<Product | null> {
    const product = this.products.get(id);
    return product ? { ...product } : null;
  }

  async findAll(page: number, size: number): Promise<{ products: Product[]; total: number }> {
    const allActive = Array.from(this.products.values()).filter((p) => p.active);
    const total = allActive.length;
    const start = page * size;
    const products = allActive.slice(start, start + size);
    return { products: products.map((p) => ({ ...p })), total };
  }

  async findBySku(sku: string): Promise<Product | null> {
    const found = Array.from(this.products.values()).find((p) => p.sku === sku);
    return found ? { ...found } : null;
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

    return results.map((p) => ({ ...p }));
  }

  async update(product: Product): Promise<Product> {
    this.products.set(product.id, { ...product });
    return { ...product };
  }

  async delete(id: string): Promise<void> {
    this.products.delete(id);
  }
}
