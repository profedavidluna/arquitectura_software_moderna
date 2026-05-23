// =============================================================================
// Product Service Interface (Contract)
// =============================================================================
// This interface defines the contract for the Product Service.
// 
// SOLID Principles Applied:
// - ISP (Interface Segregation): This interface is focused only on product operations
// - DIP (Dependency Inversion): Controllers depend on this abstraction, not implementations
// - LSP (Liskov Substitution): Any implementation can be substituted without breaking clients
// =============================================================================

import { Product, CreateProductData, UpdateProductData } from '../model/Product';

/**
 * IProductService - Service contract for product catalog operations.
 * 
 * In SOA, service interfaces define the contract that the service exposes.
 * This enables loose coupling between the service layer and its consumers.
 */
export interface IProductService {
  /**
   * Retrieve all active products from the catalog.
   */
  findAll(): Promise<Product[]>;

  /**
   * Find a specific product by its unique identifier.
   * @param id - Product UUID
   * @returns Product if found, null otherwise
   */
  findById(id: string): Promise<Product | null>;

  /**
   * Create a new product in the catalog.
   * Publishes a 'product.created' event to the ESB.
   * @param data - Product creation data
   * @returns The created product
   */
  create(data: CreateProductData): Promise<Product>;

  /**
   * Update an existing product.
   * @param id - Product UUID
   * @param data - Fields to update
   * @returns Updated product if found, null otherwise
   */
  update(id: string, data: UpdateProductData): Promise<Product | null>;

  /**
   * Soft-delete a product (marks as inactive).
   * @param id - Product UUID
   * @returns true if deleted, false if not found
   */
  delete(id: string): Promise<boolean>;
}
