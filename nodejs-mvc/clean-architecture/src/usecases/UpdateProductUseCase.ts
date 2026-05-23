import { Product } from '../entities/Product';
import { ProductGateway } from './interfaces/ProductGateway';
import { ProductOutput, UpdateProductInput } from './dto/ProductDTO';
import { DuplicateSkuError, ProductNotFoundError } from './errors/UseCaseErrors';

/**
 * @layer Use Cases
 * @description Updates an existing product's details.
 */
export class UpdateProductUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async execute(id: string, input: UpdateProductInput): Promise<ProductOutput> {
    const existing = await this.gateway.findById(id);
    if (!existing) {
      throw new ProductNotFoundError(id);
    }

    // Check SKU uniqueness if changing
    if (input.sku && input.sku !== existing.sku) {
      const bySku = await this.gateway.findBySku(input.sku);
      if (bySku) {
        throw new DuplicateSkuError(input.sku);
      }
    }

    // Entity handles validation of business rules
    const updated = existing.updateDetails(input);
    const saved = await this.gateway.update(updated);
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
