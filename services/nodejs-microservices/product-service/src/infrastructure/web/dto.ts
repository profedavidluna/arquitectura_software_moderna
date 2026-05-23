import { Product, Category, ProductReview } from '../../domain/models/Product';

export interface CreateProductRequest {
  name: string;
  description?: string;
  price: number;
  categoryId?: string;
  imageUrl?: string;
}

export interface UpdateProductRequest {
  name?: string;
  description?: string;
  price?: number;
  categoryId?: string;
  imageUrl?: string;
  status?: string;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  parentId?: string;
}

export interface CreateReviewRequest {
  userId: string;
  rating: number;
  comment?: string;
}

export interface ProductResponse {
  id: string;
  name: string;
  description?: string;
  price: number;
  categoryId?: string;
  imageUrl?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export function toProductResponse(product: Product): ProductResponse {
  return {
    id: product.id,
    name: product.name,
    description: product.description,
    price: product.price,
    categoryId: product.categoryId,
    imageUrl: product.imageUrl,
    status: product.status,
    createdAt: product.createdAt.toISOString(),
    updatedAt: product.updatedAt.toISOString(),
  };
}

export function toCategoryResponse(category: Category) {
  return {
    id: category.id,
    name: category.name,
    description: category.description,
    parentId: category.parentId,
    createdAt: category.createdAt.toISOString(),
  };
}

export function toReviewResponse(review: ProductReview) {
  return {
    id: review.id,
    productId: review.productId,
    userId: review.userId,
    rating: review.rating,
    comment: review.comment,
    createdAt: review.createdAt.toISOString(),
  };
}
