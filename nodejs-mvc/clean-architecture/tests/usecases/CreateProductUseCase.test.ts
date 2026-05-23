import { CreateProductUseCase } from '../../src/usecases/CreateProductUseCase';
import { InMemoryProductGateway } from '../../src/frameworks/persistence/InMemoryProductGateway';
import { Product } from '../../src/entities/Product';
import { DuplicateSkuError } from '../../src/usecases/errors/UseCaseErrors';

describe('CreateProductUseCase (Clean Architecture)', () => {
  let useCase: CreateProductUseCase;
  let gateway: InMemoryProductGateway;

  beforeEach(() => {
    gateway = new InMemoryProductGateway();
    useCase = new CreateProductUseCase(gateway);
  });

  it('should create a product successfully', async () => {
    const output = await useCase.execute({
      name: 'Laptop',
      description: 'A powerful laptop',
      price: 999.99,
      category: 'Electronics',
      stockQuantity: 50,
      sku: 'LAP-001',
    });

    expect(output.id).toBeDefined();
    expect(output.name).toBe('Laptop');
    expect(output.price).toBe(999.99);
    expect(output.active).toBe(true);
  });

  it('should reject price <= 0 (entity validation)', async () => {
    await expect(
      useCase.execute({
        name: 'Bad',
        description: 'desc',
        price: 0,
        category: 'Test',
        stockQuantity: 10,
        sku: 'BAD-001',
      })
    ).rejects.toThrow('Price must be greater than 0');
  });

  it('should reject duplicate SKU (use case rule)', async () => {
    await useCase.execute({
      name: 'Product 1',
      description: 'desc',
      price: 10,
      category: 'Test',
      stockQuantity: 5,
      sku: 'DUP-001',
    });

    await expect(
      useCase.execute({
        name: 'Product 2',
        description: 'desc',
        price: 20,
        category: 'Test',
        stockQuantity: 3,
        sku: 'DUP-001',
      })
    ).rejects.toThrow(DuplicateSkuError);
  });

  it('should reject empty name (entity validation)', async () => {
    await expect(
      useCase.execute({
        name: '',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 5,
        sku: 'EMPTY-001',
      })
    ).rejects.toThrow('Product name is required');
  });
});
