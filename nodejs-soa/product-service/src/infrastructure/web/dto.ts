// =============================================================================
// Data Transfer Objects (DTOs) - Product Service
// =============================================================================
// DTOs define the shape of data exchanged over the API.
// They decouple the API contract from the internal domain model.
//
// Why DTOs?
// - API stability: internal model can change without breaking clients
// - Validation: DTOs can include validation rules
// - Security: prevents exposing internal fields (e.g., database IDs)
// =============================================================================

/**
 * Request body for creating a product.
 */
export interface CreateProductDto {
  name: string;
  description?: string;
  price: number;
  category?: string;
  sku: string;
}

/**
 * Request body for updating a product.
 */
export interface UpdateProductDto {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  active?: boolean;
}

/**
 * Response DTO for a product.
 */
export interface ProductResponseDto {
  id: string;
  name: string;
  description: string | null;
  price: number;
  category: string | null;
  sku: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Validates the create product request body.
 * Returns an error message if invalid, null if valid.
 */
export function validateCreateProductDto(body: any): string | null {
  if (!body.name || typeof body.name !== 'string') {
    return 'name is required and must be a string';
  }
  if (body.price === undefined || typeof body.price !== 'number' || body.price <= 0) {
    return 'price is required and must be a positive number';
  }
  if (!body.sku || typeof body.sku !== 'string') {
    return 'sku is required and must be a string';
  }
  return null;
}
