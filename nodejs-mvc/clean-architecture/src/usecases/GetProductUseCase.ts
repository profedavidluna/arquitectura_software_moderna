import { Product } from '../entities/Product';
import { ProductGateway } from './interfaces/ProductGateway';
import { ProductOutput } from './dto/ProductDTO';
import { ProductNotFoundError } from './errors/UseCaseErrors';

/**
 * @layer Use Cases
 * @description Retrieves a single product by ID.
 */
export class GetProductUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async execute(id: string): Promise<ProductOutput> {
    const product = await this.gateway.findById(id);
    if (!product) {
      throw new ProductNotFoundError(id);
    }
    return this.toOutput(product);
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
