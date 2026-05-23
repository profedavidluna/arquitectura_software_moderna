import { Product } from '../../model/Product';

/**
 * @layer Domain - Output Port
 * @description Defines the operations that the domain needs from the outside world.
 * This is the SECONDARY/DRIVEN port in Hexagonal Architecture.
 *
 * Output ports define WHAT the domain needs from persistence, without
 * specifying HOW it's implemented (in-memory, SQL, NoSQL, etc.)
 * The domain service depends on this interface, NOT on concrete implementations.
 */
export interface ProductRepositoryPort {
  save(product: Product): Promise<Product>;
  findById(id: string): Promise<Product | null>;
  findAll(page: number, size: number): Promise<{ products: Product[]; total: number }>;
  findBySku(sku: string): Promise<Product | null>;
  search(query?: string, category?: string, minPrice?: number, maxPrice?: number): Promise<Product[]>;
  update(product: Product): Promise<Product>;
  delete(id: string): Promise<void>;
}
