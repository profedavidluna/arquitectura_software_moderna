import { Product } from '../../entities/Product';

/**
 * @layer Use Cases - Gateway Interface
 * @description Defines the contract for data access that use cases need.
 * In Clean Architecture, this is the INTERFACE that the outer layers must implement.
 *
 * The Dependency Rule: source code dependencies point INWARD.
 * Use cases define what they need; outer layers provide the implementation.
 */
export interface ProductGateway {
  save(product: Product): Promise<Product>;
  findById(id: string): Promise<Product | null>;
  findAll(page: number, size: number): Promise<{ products: Product[]; total: number }>;
  findBySku(sku: string): Promise<Product | null>;
  search(query?: string, category?: string, minPrice?: number, maxPrice?: number): Promise<Product[]>;
  update(product: Product): Promise<Product>;
  delete(id: string): Promise<void>;
}
