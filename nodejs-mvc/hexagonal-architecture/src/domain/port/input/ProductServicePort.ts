import { Product } from '../../model/Product';

/**
 * @layer Domain - Input Port
 * @description Defines the operations that the application offers to the outside world.
 * This is the PRIMARY/DRIVING port in Hexagonal Architecture.
 *
 * Input ports define WHAT the application can do, without specifying HOW.
 * The web adapter (controller) calls these methods to interact with the domain.
 */

export interface CreateProductCommand {
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
}

export interface UpdateProductCommand {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  stockQuantity?: number;
  sku?: string;
}

export interface SearchCriteria {
  query?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
}

export interface PaginatedResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductServicePort {
  createProduct(command: CreateProductCommand): Promise<Product>;
  getProductById(id: string): Promise<Product>;
  listProducts(page: number, size: number): Promise<PaginatedResult<Product>>;
  searchProducts(criteria: SearchCriteria): Promise<Product[]>;
  updateProduct(id: string, command: UpdateProductCommand): Promise<Product>;
  deleteProduct(id: string): Promise<void>;
  decreaseStock(id: string, quantity: number): Promise<Product>;
  increaseStock(id: string, quantity: number): Promise<Product>;
}
