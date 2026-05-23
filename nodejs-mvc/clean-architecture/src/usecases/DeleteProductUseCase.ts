import { ProductGateway } from './interfaces/ProductGateway';
import { ProductNotFoundError } from './errors/UseCaseErrors';

/**
 * @layer Use Cases
 * @description Soft-deletes a product (deactivates it).
 */
export class DeleteProductUseCase {
  constructor(private readonly gateway: ProductGateway) {}

  async execute(id: string): Promise<void> {
    const product = await this.gateway.findById(id);
    if (!product) {
      throw new ProductNotFoundError(id);
    }

    const deactivated = product.deactivate();
    await this.gateway.update(deactivated);
  }
}
