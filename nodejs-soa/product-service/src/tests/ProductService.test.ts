// =============================================================================
// Product Service - Unit Tests
// =============================================================================
// Tests use Jest with mocked dependencies (no real Kafka/DB needed).
// This demonstrates the benefit of DIP: we can test business logic in isolation.
// =============================================================================

import { ProductService } from '../application/ProductService';
import { ProductRepository } from '../infrastructure/persistence/ProductRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { Product, CreateProductData } from '../domain/model/Product';

// Mock the dependencies
jest.mock('../infrastructure/persistence/ProductRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');

describe('ProductService', () => {
  let productService: ProductService;
  let mockRepository: jest.Mocked<ProductRepository>;
  let mockKafkaProducer: jest.Mocked<KafkaProducer>;

  const mockProduct: Product = {
    id: 'test-uuid-123',
    name: 'Test Product',
    description: 'A test product',
    price: 99.99,
    category: 'Testing',
    sku: 'TEST-001',
    active: true,
    createdAt: new Date('2024-01-01'),
    updatedAt: new Date('2024-01-01'),
  };

  beforeEach(() => {
    // Create fresh mocks for each test
    mockRepository = new ProductRepository(null as any) as jest.Mocked<ProductRepository>;
    mockKafkaProducer = new KafkaProducer([], '') as jest.Mocked<KafkaProducer>;

    productService = new ProductService(mockRepository, mockKafkaProducer);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('findAll', () => {
    it('should return all products from repository', async () => {
      mockRepository.findAll = jest.fn().mockResolvedValue([mockProduct]);

      const result = await productService.findAll();

      expect(result).toEqual([mockProduct]);
      expect(mockRepository.findAll).toHaveBeenCalledTimes(1);
    });

    it('should return empty array when no products exist', async () => {
      mockRepository.findAll = jest.fn().mockResolvedValue([]);

      const result = await productService.findAll();

      expect(result).toEqual([]);
    });
  });

  describe('findById', () => {
    it('should return product when found', async () => {
      mockRepository.findById = jest.fn().mockResolvedValue(mockProduct);

      const result = await productService.findById('test-uuid-123');

      expect(result).toEqual(mockProduct);
      expect(mockRepository.findById).toHaveBeenCalledWith('test-uuid-123');
    });

    it('should return null when product not found', async () => {
      mockRepository.findById = jest.fn().mockResolvedValue(null);

      const result = await productService.findById('non-existent');

      expect(result).toBeNull();
    });
  });

  describe('create', () => {
    const createData: CreateProductData = {
      name: 'New Product',
      description: 'A new product',
      price: 49.99,
      category: 'New',
      sku: 'NEW-001',
    };

    it('should create product and publish event', async () => {
      mockRepository.create = jest.fn().mockResolvedValue(mockProduct);
      mockKafkaProducer.publish = jest.fn().mockResolvedValue(undefined);

      const result = await productService.create(createData);

      expect(result).toEqual(mockProduct);
      expect(mockRepository.create).toHaveBeenCalledWith(createData);
      expect(mockKafkaProducer.publish).toHaveBeenCalledWith(
        'product.created',
        mockProduct.id,
        expect.objectContaining({
          eventType: 'PRODUCT_CREATED',
          data: expect.objectContaining({
            productId: mockProduct.id,
            name: mockProduct.name,
          }),
        })
      );
    });

    it('should still return product even if Kafka publish fails', async () => {
      mockRepository.create = jest.fn().mockResolvedValue(mockProduct);
      mockKafkaProducer.publish = jest.fn().mockRejectedValue(new Error('Kafka down'));

      const result = await productService.create(createData);

      // Product should still be created even if event publishing fails
      expect(result).toEqual(mockProduct);
    });
  });

  describe('update', () => {
    it('should update product when found', async () => {
      const updatedProduct = { ...mockProduct, name: 'Updated Name' };
      mockRepository.update = jest.fn().mockResolvedValue(updatedProduct);

      const result = await productService.update('test-uuid-123', { name: 'Updated Name' });

      expect(result).toEqual(updatedProduct);
      expect(mockRepository.update).toHaveBeenCalledWith('test-uuid-123', { name: 'Updated Name' });
    });

    it('should return null when product not found', async () => {
      mockRepository.update = jest.fn().mockResolvedValue(null);

      const result = await productService.update('non-existent', { name: 'Updated' });

      expect(result).toBeNull();
    });
  });

  describe('delete', () => {
    it('should return true when product is deleted', async () => {
      mockRepository.delete = jest.fn().mockResolvedValue(true);

      const result = await productService.delete('test-uuid-123');

      expect(result).toBe(true);
      expect(mockRepository.delete).toHaveBeenCalledWith('test-uuid-123');
    });

    it('should return false when product not found', async () => {
      mockRepository.delete = jest.fn().mockResolvedValue(false);

      const result = await productService.delete('non-existent');

      expect(result).toBe(false);
    });
  });
});
