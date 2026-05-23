/**
 * @layer Adapter - Input (Primary/Driving) - DTOs
 * @description Data Transfer Objects for the web adapter.
 * These types define the shape of HTTP request/response bodies.
 * They are specific to the web layer and should NOT leak into the domain.
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
