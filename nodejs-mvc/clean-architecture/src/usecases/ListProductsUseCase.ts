import { Product } from '../entities/Product';
import { ProductGateway } from './interfaces/ProductGateway';
import { PaginatedOutput, ProductOutput } from './dto/ProductDTO';

/**
 * @layer Use Cases
 * @description Lists products with pagination.
 */
export class ListProductsUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async execute(page: number, size: number): Promise<PaginatedOutput<ProductOutput>> {
    const effectiveSize = Math.min(size, 100);
    const effectivePage = Math.max(page, 0);

    const { products, total } = await this.gateway.findAll(effectivePage, effectiveSize);

    return {
      content: products.map(this.toOutput),
      page: effectivePage,
      size: effectiveSize,
      totalElements: total,
      totalPages: Math.ceil(total / effectiveSize),
    };
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
