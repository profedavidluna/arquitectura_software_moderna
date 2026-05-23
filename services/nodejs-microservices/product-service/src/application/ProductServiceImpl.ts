/**
 * Product Service Implementation
 * 
 * Implements Cache-Aside pattern for product reads:
 * - Read: Check Redis → if miss, query DB → store in Redis
 * - Write: Update DB → invalidate Redis cache
 * 
 * This ensures high read performance while maintaining consistency.
 */
import { v4 as uuidv4 } from 'uuid';
import { Product, Category, ProductReview, ProductStatus } from '../domain/models/Product';
import { IProductService, CreateProductDTO, UpdateProductDTO, CreateCategoryDTO, CreateReviewDTO } from '../domain/interfaces/IProductService';
import { ProductRepository } from '../infrastructure/persistence/ProductRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { RedisClient } from '../infrastructure/cache/RedisClient';
import { PRODUCT_EVENTS_TOPIC, ProductEventType } from '../infrastructure/messaging/events';

export class ProductServiceImpl implements IProductService {
  constructor(
    private readonly productRepository: ProductRepository,
    private readonly kafkaProducer: KafkaProducer,
    private readonly redisClient: RedisClient
  ) {}

  async createProduct(dto: CreateProductDTO): Promise<Product> {
    const product: Omit<Product, 'createdAt' | 'updatedAt'> = {
      id: uuidv4(),
      name: dto.name,
      description: dto.description,
      price: dto.price,
      categoryId: dto.categoryId,
      imageUrl: dto.imageUrl,
      status: ProductStatus.ACTIVE,
    };

    const created = await this.productRepository.create(product);

    // Cache the new product
    await this.redisClient.set(`product:${created.id}`, created);

    await this.kafkaProducer.publish(PRODUCT_EVENTS_TOPIC, {
      type: ProductEventType.PRODUCT_CREATED,
      data: { id: created.id, name: created.name, price: created.price },
    });

    return created;
  }

  async getProductById(id: string): Promise<Product | null> {
    // Cache-Aside: check cache first
    const cached = await this.redisClient.get<Product>(`product:${id}`);
    if (cached) {
      console.log(`[Cache] Hit for product:${id}`);
      return cached;
    }

    // Cache miss: load from database
    const product = await this.productRepository.findById(id);
    if (product) {
      // Store in cache for future requests
      await this.redisClient.set(`product:${id}`, product);
    }
    return product;
  }

  async updateProduct(id: string, dto: UpdateProductDTO): Promise<Product | null> {
    const existing = await this.productRepository.findById(id);
    if (!existing) return null;

    const updated = await this.productRepository.update(id, {
      name: dto.name,
      description: dto.description,
      price: dto.price,
      categoryId: dto.categoryId,
      imageUrl: dto.imageUrl,
      status: dto.status as ProductStatus,
    });

    if (updated) {
      // Invalidate cache on write
      await this.redisClient.delete(`product:${id}`);

      // Publish price change event if price changed
      if (dto.price && dto.price !== existing.price) {
        await this.kafkaProducer.publish(PRODUCT_EVENTS_TOPIC, {
          type: ProductEventType.PRICE_CHANGED,
          data: { id: updated.id, oldPrice: existing.price, newPrice: updated.price },
        });
      }

      await this.kafkaProducer.publish(PRODUCT_EVENTS_TOPIC, {
        type: ProductEventType.PRODUCT_UPDATED,
        data: { id: updated.id, name: updated.name, price: updated.price },
      });
    }

    return updated;
  }

  async deleteProduct(id: string): Promise<boolean> {
    const deleted = await this.productRepository.delete(id);
    if (deleted) {
      await this.redisClient.delete(`product:${id}`);
      await this.kafkaProducer.publish(PRODUCT_EVENTS_TOPIC, {
        type: ProductEventType.PRODUCT_DELETED,
        data: { id },
      });
    }
    return deleted;
  }

  async listProducts(page: number, limit: number): Promise<Product[]> {
    const offset = (page - 1) * limit;
    return this.productRepository.findAll(offset, limit);
  }

  async searchProducts(query: string): Promise<Product[]> {
    return this.productRepository.search(query);
  }

  async createCategory(dto: CreateCategoryDTO): Promise<Category> {
    const category: Omit<Category, 'createdAt'> = {
      id: uuidv4(),
      name: dto.name,
      description: dto.description,
      parentId: dto.parentId,
    };
    return this.productRepository.createCategory(category);
  }

  async listCategories(): Promise<Category[]> {
    return this.productRepository.findAllCategories();
  }

  async addReview(productId: string, dto: CreateReviewDTO): Promise<ProductReview> {
    const product = await this.productRepository.findById(productId);
    if (!product) throw new Error('Product not found');

    const review: Omit<ProductReview, 'createdAt'> = {
      id: uuidv4(),
      productId,
      userId: dto.userId,
      rating: dto.rating,
      comment: dto.comment,
    };

    return this.productRepository.createReview(review);
  }

  async getReviews(productId: string): Promise<ProductReview[]> {
    return this.productRepository.findReviewsByProductId(productId);
  }
}
