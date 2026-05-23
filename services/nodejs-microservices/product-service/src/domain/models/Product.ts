/**
 * Product Domain Models
 */
export interface Product {
  id: string;
  name: string;
  description?: string;
  price: number;
  categoryId?: string;
  imageUrl?: string;
  status: ProductStatus;
  createdAt: Date;
  updatedAt: Date;
}

export enum ProductStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  DISCONTINUED = 'DISCONTINUED',
}

export interface Category {
  id: string;
  name: string;
  description?: string;
  parentId?: string;
  createdAt: Date;
}

export interface ProductReview {
  id: string;
  productId: string;
  userId: string;
  rating: number;
  comment?: string;
  createdAt: Date;
}
