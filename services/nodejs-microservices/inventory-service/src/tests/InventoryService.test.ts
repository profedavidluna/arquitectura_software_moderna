import { InventoryServiceImpl } from '../application/InventoryServiceImpl';
import { InventoryRepository } from '../infrastructure/persistence/InventoryRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { ReservationStatus } from '../domain/models/Inventory';

jest.mock('../infrastructure/persistence/InventoryRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');

describe('InventoryServiceImpl', () => {
  let service: InventoryServiceImpl;
  let mockRepo: jest.Mocked<InventoryRepository>;
  let mockKafka: jest.Mocked<KafkaProducer>;

  beforeEach(() => {
    mockRepo = new InventoryRepository() as jest.Mocked<InventoryRepository>;
    mockKafka = new KafkaProducer() as jest.Mocked<KafkaProducer>;
    service = new InventoryServiceImpl(mockRepo, mockKafka);
  });

  afterEach(() => jest.clearAllMocks());

  describe('reserveStock', () => {
    it('should reserve stock when sufficient quantity available', async () => {
      mockRepo.reserveStock.mockResolvedValue({
        id: 'inv-1', productId: 'prod-1', quantity: 45,
        reservedQuantity: 5, reorderLevel: 10, updatedAt: new Date(),
      });
      mockRepo.createReservation.mockResolvedValue({
        id: 'res-1', orderId: 'order-1', productId: 'prod-1',
        quantity: 5, status: ReservationStatus.ACTIVE, createdAt: new Date(),
      });
      mockKafka.publish.mockResolvedValue();

      const result = await service.reserveStock('prod-1', { orderId: 'order-1', quantity: 5 });

      expect(result.quantity).toBe(5);
      expect(result.status).toBe(ReservationStatus.ACTIVE);
      expect(mockKafka.publish).toHaveBeenCalledWith('inventory-events', expect.objectContaining({ type: 'INVENTORY_RESERVED' }));
    });

    it('should throw error when insufficient stock', async () => {
      mockRepo.reserveStock.mockResolvedValue(null); // Atomic update failed
      mockKafka.publish.mockResolvedValue();

      await expect(
        service.reserveStock('prod-1', { orderId: 'order-1', quantity: 100 })
      ).rejects.toThrow('Insufficient stock');

      expect(mockKafka.publish).toHaveBeenCalledWith('inventory-events', expect.objectContaining({ type: 'INVENTORY_RESERVATION_FAILED' }));
    });

    it('should publish low stock alert when below reorder level', async () => {
      mockRepo.reserveStock.mockResolvedValue({
        id: 'inv-1', productId: 'prod-1', quantity: 5, // Below reorderLevel of 10
        reservedQuantity: 45, reorderLevel: 10, updatedAt: new Date(),
      });
      mockRepo.createReservation.mockResolvedValue({
        id: 'res-1', orderId: 'order-1', productId: 'prod-1',
        quantity: 5, status: ReservationStatus.ACTIVE, createdAt: new Date(),
      });
      mockKafka.publish.mockResolvedValue();

      await service.reserveStock('prod-1', { orderId: 'order-1', quantity: 5 });

      // Should publish both RESERVED and LOW_STOCK_ALERT
      expect(mockKafka.publish).toHaveBeenCalledTimes(2);
      expect(mockKafka.publish).toHaveBeenCalledWith('inventory-events', expect.objectContaining({ type: 'LOW_STOCK_ALERT' }));
    });
  });

  describe('releaseStock', () => {
    it('should release reserved stock', async () => {
      mockRepo.findReservationsByOrderId.mockResolvedValue([{
        id: 'res-1', orderId: 'order-1', productId: 'prod-1',
        quantity: 5, status: ReservationStatus.ACTIVE, createdAt: new Date(),
      }]);
      mockRepo.releaseStock.mockResolvedValue({
        id: 'inv-1', productId: 'prod-1', quantity: 50,
        reservedQuantity: 0, reorderLevel: 10, updatedAt: new Date(),
      });
      mockRepo.updateReservationStatus.mockResolvedValue();
      mockKafka.publish.mockResolvedValue();

      const result = await service.releaseStock('prod-1', 'order-1');
      expect(result).toBe(true);
    });

    it('should return false when no reservation found', async () => {
      mockRepo.findReservationsByOrderId.mockResolvedValue([]);
      const result = await service.releaseStock('prod-1', 'order-1');
      expect(result).toBe(false);
    });
  });
});
