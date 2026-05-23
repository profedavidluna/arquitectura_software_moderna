// =============================================================================
// Product Domain Entity
// =============================================================================
// The domain model represents the core business concept.
// In SOA, domain entities are internal to the service and never exposed directly.
// External communication uses DTOs (Data Transfer Objects).
// =============================================================================

/**
 * Product domain entity - represents a product in the catalog.
 * This is the core business object that the Product Service manages.
 */
export interface Product {
  id: string;
  name: string;
  description: string | null;
  price: number;
  category: string | null;
  sku: string;
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Data required to create a new product.
 * Separating creation data from the full entity follows the
 * Command Query Responsibility Segregation (CQRS) principle.
 */
export interface CreateProductData {
  name: string;
  description?: string;
  price: number;
  category?: string;
  sku: string;
}

/**
 * Data for updating an existing product.
 * All fields are optional - only provided fields will be updated.
 */
export interface UpdateProductData {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  active?: boolean;
}
