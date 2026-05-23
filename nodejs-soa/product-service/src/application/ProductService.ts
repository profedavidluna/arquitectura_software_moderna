// =============================================================================
// Product Service Implementation
// =============================================================================
// This class implements the IProductService interface.
// 
// SOLID Principles Applied:
// - SRP (Single Responsibility): Only handles product business logic
// - DIP (Dependency Inversion): Depends on repository abstraction, not concrete DB
// - OCP (Open/Closed): New features can be added via new methods without modifying existing ones
//
// SOA Concepts:
// - Service Layer Pattern: Encapsulates business logic
// - Event Publishing: Notifies other services of state changes via ESB
// =============================================================================

import { Product, CreateProductData, UpdateProductData } from '../domain/model/Product';
import { IProductService } from '../domain/service/IProductService';
import { ProductRepository } from '../infrastructure/persistence/ProductRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { createProductCreatedEvent } from '../infrastructure/messaging/events';

export class ProductService implements IProductService {
  constructor(
    private readonly repository: ProductRepository,
    private readonly kafkaProducer: KafkaProducer
  ) {}

  async findAll(): Promise<Product[]> {
    return this.repository.findAll();
  }

  async findById(id: string): Promise<Product | null> {
    return this.repository.findById(id);
  }

  async create(data: CreateProductData): Promise<Product> {
    // 1. Persist the product in the database
    const product = await this.repository.create(data);

    // 2. Publish event to ESB (Kafka) - other services can react to this
    // This is the Observer/Pub-Sub pattern in action
    try {
      const event = createProductCreatedEvent(product);
      await this.kafkaProducer.publish('product.created', product.id, event);
      console.log(`[ProductService] Published product.created event for product: ${product.id}`);
    } catch (error) {
      // Event publishing failure should not fail the operation
      // In production, you'd use an outbox pattern for guaranteed delivery
      console.error(`[ProductService] Failed to publish product.created event:`, error);
    }

    return product;
  }

  async update(id: string, data: UpdateProductData): Promise<Product | null> {
    return this.repository.update(id, data);
  }

  async delete(id: string): Promise<boolean> {
    return this.repository.delete(id);
  }
}
