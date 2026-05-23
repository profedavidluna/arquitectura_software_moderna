import { Product } from '../entities/Product';
import { ProductGateway } from './interfaces/ProductGateway';
import { ProductOutput } from './dto/ProductDTO';
import { ProductNotFoundError } from './errors/UseCaseErrors';

/**
 * @layer Use Cases
 * @description Manages product stock (increase/decrease).
 */
export class ManageStockUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async decrease(id: string, quantity: number): Promise<ProductOutput> {
    const product = await this.gateway.findById(id);
    if (!product) {
      throw new ProductNotFoundError(id);
    }

    const updated = product.decreaseStock(quantity);
    const saved = await this.gateway.update(updated);
    return this.toOutput(saved);
  }

  async increase(id: string, quantity: number): Promise<ProductOutput> {
    const product = await this.gateway.findById(id);
    if (!product) {
      throw new ProductNotFoundError(id);
    }

    const updated = product.increaseStock(quantity);
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
