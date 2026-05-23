/**
 * @layer Presentation Layer - DTOs
 * @description Data Transfer Objects for HTTP request/response bodies.
 * In Layered Architecture, DTOs are defined in the Presentation layer
 * to shape the API contract independently from the data model.
 */

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
}

export interface UpdateProductRequest {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  stockQuantity?: number;
  sku?: string;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
