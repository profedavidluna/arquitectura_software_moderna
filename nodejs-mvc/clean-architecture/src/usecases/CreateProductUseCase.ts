import { v4 as uuidv4 } from 'uuid';
import { Product } from '../entities/Product';
import { ProductGateway } from './interfaces/ProductGateway';
import { CreateProductInput, ProductOutput } from './dto/ProductDTO';
import { DuplicateSkuError } from './errors/UseCaseErrors';

/**
 * @layer Use Cases (Application Business Rules)
 * @description Orchestrates the creation of a new product.
 * Each use case class has a single responsibility (SRP).
 *
 * In Clean Architecture, use cases:
 * - Contain application-specific business rules
 * - Orchestrate the flow of data to/from entities
 * - Depend on entity layer (inward) and gateway interfaces (same layer)
 * - Do NOT depend on frameworks, UI, or databases
 */
export class CreateProductUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async execute(input: CreateProductInput): Promise<ProductOutput> {
    // Check SKU uniqueness (application rule)
    const existing = await this.gateway.findBySku(input.sku);
    if (existing) {
      throw new DuplicateSkuError(input.sku);
    }

    // Create entity (entity validates business rules internally)
    const now = new Date();
    const product = Product.create({
      id: uuidv4(),
      name: input.name,
      description: input.description,
      price: input.price,
      category: input.category,
      stockQuantity: input.stockQuantity,
      sku: input.sku,
      active: true,
      createdAt: now,
      updatedAt: now,
    });

    const saved = await this.gateway.save(product);
    return this.toOutput(saved);
  }

  private toOutput(product: Product): ProductOutput {
    return {
      id: product.id,
      name: product.name,
      description: product.description,
      price: product.price,
      category: product.category,
      stockQuantity: product.stockQuantity,
      sku: product.sku,
      active: product.active,
      createdAt: product.createdAt.toISOString(),
      updatedAt: product.updatedAt.toISOString(),
    };
  }
}
