import { ProductServiceImpl } from '../application/ProductServiceImpl';
import { ProductRepository } from '../infrastructure/persistence/ProductRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { RedisClient } from '../infrastructure/cache/RedisClient';
import { ProductStatus } from '../domain/models/Product';

jest.mock('../infrastructure/persistence/ProductRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');
jest.mock('../infrastructure/cache/RedisClient');

describe('ProductServiceImpl', () => {
  let service: ProductServiceImpl;
  let mockRepo: jest.Mocked<ProductRepository>;
  let mockKafka: jest.Mocked<KafkaProducer>;
  let mockRedis: jest.Mocked<RedisClient>;

  beforeEach(() => {
    mockRepo = new ProductRepository() as jest.Mocked<ProductRepository>;
    mockKafka = new KafkaProducer() as jest.Mocked<KafkaProducer>;
    mockRedis = new RedisClient() as jest.Mocked<RedisClient>;
    service = new ProductServiceImpl(mockRepo, mockKafka, mockRedis);
  });

  afterEach(() => jest.clearAllMocks());

  describe('createProduct', () => {
    it('should create product, cache it, and publish event', async () => {
      const dto = { name: 'Laptop', price: 999.99, description: 'Gaming laptop' };
      mockRepo.create.mockResolvedValue({
        id: 'prod-1', name: 'Laptop', description: 'Gaming laptop',
        price: 999.99, status: ProductStatus.ACTIVE,
        createdAt: new Date(), updatedAt: new Date(),
      });
      mockRedis.set.mockResolvedValue();
      mockKafka.publish.mockResolvedValue();

      const result = await service.createProduct(dto);

      expect(result.name).toBe('Laptop');
      expect(mockRedis.set).toHaveBeenCalledWith(`product:${result.id}`, result);
      expect(mockKafka.publish).toHaveBeenCalledWith('product-events', expect.objectContaining({ type: 'PRODUCT_CREATED' }));
    });
  });

  describe('getProductById', () => {
    it('should return cached product on cache hit', async () => {
      const cachedProduct = {
        id: 'prod-1', name: 'Laptop', price: 999.99,
        status: ProductStatus.ACTIVE, createdAt: new Date(), updatedAt: new Date(),
      };
      mockRedis.get.mockResolvedValue(cachedProduct);

      const result = await service.getProductById('prod-1');

      expect(result).toEqual(cachedProduct);
      expect(mockRepo.findById).not.toHaveBeenCalled(); // DB not queried
    });

    it('should query DB and cache on cache miss', async () => {
      const product = {
        id: 'prod-1', name: 'Laptop', price: 999.99,
        status: ProductStatus.ACTIVE, createdAt: new Date(), updatedAt: new Date(),
      };
      mockRedis.get.mockResolvedValue(null); // Cache miss
      mockRepo.findById.mockResolvedValue(product);
      mockRedis.set.mockResolvedValue();

      const result = await service.getProductById('prod-1');

      expect(result).toEqual(product);
      expect(mockRepo.findById).toHaveBeenCalledWith('prod-1');
      expect(mockRedis.set).toHaveBeenCalledWith('product:prod-1', product);
    });
  });

  describe('updateProduct', () => {
    it('should invalidate cache after update', async () => {
      mockRepo.findById.mockResolvedValue({
        id: 'prod-1', name: 'Laptop', price: 999.99,
        status: ProductStatus.ACTIVE, createdAt: new Date(), updatedAt: new Date(),
      });
      mockRepo.update.mockResolvedValue({
        id: 'prod-1', name: 'Updated Laptop', price: 899.99,
        status: ProductStatus.ACTIVE, createdAt: new Date(), updatedAt: new Date(),
      });
      mockRedis.delete.mockResolvedValue();
      mockKafka.publish.mockResolvedValue();

      await service.updateProduct('prod-1', { name: 'Updated Laptop', price: 899.99 });

      expect(mockRedis.delete).toHaveBeenCalledWith('product:prod-1');
    });
  });

  describe('searchProducts', () => {
    it('should delegate search to repository', async () => {
      mockRepo.search.mockResolvedValue([]);
      await service.searchProducts('laptop');
      expect(mockRepo.search).toHaveBeenCalledWith('laptop');
    });
  });
});
