import { Product, Category, ProductReview } from '../models/Product';

export interface CreateProductDTO {
  name: string;
  description?: string;
  price: number;
  categoryId?: string;
  imageUrl?: string;
}

export interface UpdateProductDTO {
  name?: string;
  description?: string;
  price?: number;
  categoryId?: string;
  imageUrl?: string;
  status?: string;
}

export interface CreateCategoryDTO {
  name: string;
  description?: string;
  parentId?: string;
}

export interface CreateReviewDTO {
  userId: string;
  rating: number;
  comment?: string;
}

export interface IProductService {
  createProduct(dto: CreateProductDTO): Promise<Product>;
  getProductById(id: string): Promise<Product | null>;
  updateProduct(id: string, dto: UpdateProductDTO): Promise<Product | null>;
  deleteProduct(id: string): Promise<boolean>;
  listProducts(page: number, limit: number): Promise<Product[]>;
  searchProducts(query: string): Promise<Product[]>;

  // Categories
  createCategory(dto: CreateCategoryDTO): Promise<Category>;
  listCategories(): Promise<Category[]>;

  // Reviews
  addReview(productId: string, dto: CreateReviewDTO): Promise<ProductReview>;
  getReviews(productId: string): Promise<ProductReview[]>;
}
