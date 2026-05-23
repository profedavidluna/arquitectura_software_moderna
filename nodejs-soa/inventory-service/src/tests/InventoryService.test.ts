import { InventoryServiceImpl } from '../application/InventoryService';
import { InventoryItem } from '../domain/model/InventoryItem';

// Mock dependencies
const mockRepository = {
  save: jest.fn(),
  findByProductId: jest.fn(),
  findAll: jest.fn(),
};

const mockProducer = {
  connect: jest.fn(),
  disconnect: jest.fn(),
  publish: jest.fn(),
};

describe('InventoryService (SOA)', () => {
  let service: InventoryServiceImpl;

  beforeEach(() => {
    jest.clearAllMocks();
    service = new InventoryServiceImpl(mockRepository as any, mockProducer as any);
  });

  describe('createItem', () => {
    it('should create an inventory item', async () => {
      mockRepository.save.mockImplementation((item: any) => Promise.resolve(item));

      const result = await service.createItem('prod-1', 'Laptop', 50);

      expect(result.productId).toBe('prod-1');
      expect(result.productName).toBe('Laptop');
      expect(result.quantityAvailable).toBe(50);
      expect(result.quantityReserved).toBe(0);
      expect(mockRepository.save).toHaveBeenCalledTimes(1);
    });
  });

  describe('reserveStock', () => {
    it('should reserve stock and publish event when available', async () => {
      const item = new InventoryItem({
        id: 'inv-1', productId: 'prod-1', productName: 'Laptop',
        quantityAvailable: 50, quantityReserved: 0, updatedAt: new Date(),
      });
      mockRepository.findByProductId.mockResolvedValue(item);
      mockRepository.save.mockImplementation((i: any) => Promise.resolve(i));
      mockProducer.publish.mockResolvedValue(undefined);

      const result = await service.reserveStock('prod-1', 10, 'order-1');

      expect(result).toBe(true);
      expect(mockProducer.publish).toHaveBeenCalledWith(
        'stock.reserved', 'order-1', expect.objectContaining({ eventType: 'STOCK_RESERVED' })
      );
    });

    it('should publish insufficient event when stock not available', async () => {
      const item = new InventoryItem({
        id: 'inv-1', productId: 'prod-1', productName: 'Laptop',
        quantityAvailable: 5, quantityReserved: 0, updatedAt: new Date(),
      });
      mockRepository.findByProductId.mockResolvedValue(item);
      mockProducer.publish.mockResolvedValue(undefined);

      const result = await service.reserveStock('prod-1', 10, 'order-1');

      expect(result).toBe(false);
      expect(mockProducer.publish).toHaveBeenCalledWith(
        'stock.insufficient', 'order-1', expect.objectContaining({ eventType: 'STOCK_INSUFFICIENT' })
      );
    });

    it('should publish insufficient when product not found', async () => {
      mockRepository.findByProductId.mockResolvedValue(null);
      mockProducer.publish.mockResolvedValue(undefined);

      const result = await service.reserveStock('prod-999', 10, 'order-1');

      expect(result).toBe(false);
      expect(mockProducer.publish).toHaveBeenCalledWith(
        'stock.insufficient', 'order-1', expect.objectContaining({ eventType: 'STOCK_INSUFFICIENT' })
      );
    });
  });

  describe('releaseStock', () => {
    it('should release stock and publish event', async () => {
      const item = new InventoryItem({
        id: 'inv-1', productId: 'prod-1', productName: 'Laptop',
        quantityAvailable: 40, quantityReserved: 10, updatedAt: new Date(),
      });
      mockRepository.findByProductId.mockResolvedValue(item);
      mockRepository.save.mockImplementation((i: any) => Promise.resolve(i));
      mockProducer.publish.mockResolvedValue(undefined);

      await service.releaseStock('prod-1', 10, 'order-1');

      expect(mockProducer.publish).toHaveBeenCalledWith(
        'stock.released', 'order-1', expect.objectContaining({ eventType: 'STOCK_RELEASED' })
      );
    });
  });
});
