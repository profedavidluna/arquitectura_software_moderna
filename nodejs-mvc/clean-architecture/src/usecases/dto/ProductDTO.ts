/**
 * @layer Use Cases - Data Transfer Objects
 * @description Input/Output DTOs for use cases.
 * These define the data that flows in and out of use cases,
 * keeping the entities isolated from external concerns.
 */

export interface CreateProductInput {
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
}

export interface UpdateProductInput {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  stockQuantity?: number;
  sku?: string;
}

export interface ProductOutput {
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

export interface PaginatedOutput<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SearchInput {
  query?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
}
