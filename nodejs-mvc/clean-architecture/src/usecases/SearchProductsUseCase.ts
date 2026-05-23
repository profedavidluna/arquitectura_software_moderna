import { Product } from '../entities/Product';
import { ProductGateway } from './interfaces/ProductGateway';
import { ProductOutput, SearchInput } from './dto/ProductDTO';

/**
 * @layer Use Cases
 * @description Searches products by criteria.
 */
export class SearchProductsUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async execute(input: SearchInput): Promise<ProductOutput[]> {
    const products = await this.gateway.search(
      input.query,
      input.category,
      input.minPrice,
      input.maxPrice
    );
    return products.map(this.toOutput);
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
