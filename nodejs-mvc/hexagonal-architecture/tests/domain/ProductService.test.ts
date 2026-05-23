import { ProductService } from '../../src/domain/service/ProductService';
import { InMemoryProductRepository } from '../../src/adapter/output/persistence/InMemoryProductRepository';

describe('ProductService (Hexagonal Architecture)', () => {
  let service: ProductService;
  let repository: InMemoryProductRepository;

  beforeEach(() => {
    repository = new InMemoryProductRepository();
    service = new ProductService(repository);
  });

  describe('createProduct', () => {
    it('should create a product successfully', async () => {
      const product = await service.createProduct({
        name: 'Laptop',
        description: 'A powerful laptop',
        price: 999.99,
        category: 'Electronics',
        stockQuantity: 50,
        sku: 'LAP-001',
      });

      expect(product.id).toBeDefined();
      expect(product.name).toBe('Laptop');
      expect(product.price).toBe(999.99);
      expect(product.active).toBe(true);
    });

    it('should reject price <= 0', async () => {
      await expect(
        service.createProduct({
          name: 'Bad Product',
          description: 'desc',
          price: 0,
          category: 'Test',
          stockQuantity: 10,
          sku: 'BAD-001',
        })
      ).rejects.toThrow('Price must be greater than 0');
    });

    it('should reject negative stock', async () => {
      await expect(
        service.createProduct({
          name: 'Bad Product',
          description: 'desc',
          price: 10,
          category: 'Test',
          stockQuantity: -1,
          sku: 'BAD-002',
        })
      ).rejects.toThrow('Stock quantity cannot be negative');
    });

    it('should reject duplicate SKU', async () => {
      await service.createProduct({
        name: 'Product 1',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 5,
        sku: 'DUP-001',
      });

      await expect(
        service.createProduct({
          name: 'Product 2',
          description: 'desc',
          price: 20,
          category: 'Test',
          stockQuantity: 3,
          sku: 'DUP-001',
        })
      ).rejects.toThrow("Product with SKU 'DUP-001' already exists");
    });
  });

  describe('getProductById', () => {
    it('should return product when found', async () => {
      const created = await service.createProduct({
        name: 'Test',
        description: 'desc',
        price: 10,
        category: 'Cat',
        stockQuantity: 5,
        sku: 'TST-001',
      });

      const found = await service.getProductById(created.id);
      expect(found.id).toBe(created.id);
    });

    it('should throw when product not found', async () => {
      await expect(service.getProductById('non-existent')).rejects.toThrow('not found');
    });
  });

  describe('listProducts', () => {
    it('should return paginated results', async () => {
      for (let i = 0; i < 25; i++) {
        await service.createProduct({
          name: `Product ${i}`,
          description: 'desc',
          price: 10 + i,
          category: 'Test',
          stockQuantity: 5,
          sku: `SKU-${i}`,
        });
      }

      const result = await service.listProducts(0, 10);
      expect(result.content.length).toBe(10);
      expect(result.totalElements).toBe(25);
      expect(result.totalPages).toBe(3);
    });

    it('should cap page size at 100', async () => {
      const result = await service.listProducts(0, 200);
      expect(result.size).toBe(100);
    });
  });

  describe('decreaseStock', () => {
    it('should decrease stock successfully', async () => {
      const product = await service.createProduct({
        name: 'Stock Test',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 20,
        sku: 'STK-001',
      });

      const updated = await service.decreaseStock(product.id, 5);
      expect(updated.stockQuantity).toBe(15);
    });

    it('should reject when insufficient stock', async () => {
      const product = await service.createProduct({
        name: 'Stock Test',
        description: 'desc',
        price: 10,
        category: 'Test',
        stockQuantity: 3,
        sku: 'STK-002',
      });

      await expect(service.decreaseStock(product.id, 5)).rejects.toThrow('Insufficient stock');
    });
  });

  describe('searchProducts', () => {
    beforeEach(async () => {
      await service.createProduct({ name: 'Laptop Pro', description: 'High-end laptop', price: 1500, category: 'Electronics', stockQuantity: 10, sku: 'LP-001' });
      await service.createProduct({ name: 'Mouse Wireless', description: 'Ergonomic mouse', price: 30, category: 'Electronics', stockQuantity: 100, sku: 'MW-001' });
      await service.createProduct({ name: 'Desk Chair', description: 'Comfortable chair', price: 250, category: 'Furniture', stockQuantity: 20, sku: 'DC-001' });
    });

    it('should search by name', async () => {
      const results = await service.searchProducts({ query: 'laptop' });
      expect(results.length).toBe(1);
      expect(results[0].name).toBe('Laptop Pro');
    });

    it('should filter by category', async () => {
      const results = await service.searchProducts({ category: 'Electronics' });
      expect(results.length).toBe(2);
    });

    it('should filter by price range', async () => {
      const results = await service.searchProducts({ minPrice: 100, maxPrice: 1000 });
      expect(results.length).toBe(1);
      expect(results[0].name).toBe('Desk Chair');
    });
  });
});
