import { CreateProductUseCase } from '../../src/usecases/CreateProductUseCase';
import { ManageStockUseCase } from '../../src/usecases/ManageStockUseCase';
import { InMemoryProductGateway } from '../../src/frameworks/persistence/InMemoryProductGateway';
import { ProductNotFoundError } from '../../src/usecases/errors/UseCaseErrors';

describe('ManageStockUseCase (Clean Architecture)', () => {
  let createUseCase: CreateProductUseCase;
  let manageStock: ManageStockUseCase;
  let gateway: InMemoryProductGateway;

  beforeEach(() => {
    gateway = new InMemoryProductGateway();
    createUseCase = new CreateProductUseCase(gateway);
    manageStock = new ManageStockUseCase(gateway);
  });

  describe('decrease', () => {
    it('should decrease stock successfully', async () => {
      const product = await createUseCase.execute({
        name: 'Test',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 20,
        sku: 'STK-001',
      });

      const result = await manageStock.decrease(product.id, 5);
      expect(result.stockQuantity).toBe(15);
    });

    it('should throw when insufficient stock', async () => {
      const product = await createUseCase.execute({
        name: 'Test',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 3,
        sku: 'STK-002',
      });

      await expect(manageStock.decrease(product.id, 10)).rejects.toThrow('Insufficient stock');
    });

    it('should throw ProductNotFoundError for non-existent product', async () => {
      await expect(manageStock.decrease('non-existent', 5)).rejects.toThrow(ProductNotFoundError);
    });
  });

  describe('increase', () => {
    it('should increase stock successfully', async () => {
      const product = await createUseCase.execute({
        name: 'Test',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 10,
        sku: 'STK-003',
      });

      const result = await manageStock.increase(product.id, 5);
      expect(result.stockQuantity).toBe(15);
    });

    it('should reject quantity <= 0', async () => {
      const product = await createUseCase.execute({
        name: 'Test',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 10,
        sku: 'STK-004',
      });

      await expect(manageStock.increase(product.id, 0)).rejects.toThrow('Quantity must be greater than 0');
    });
  });
});
