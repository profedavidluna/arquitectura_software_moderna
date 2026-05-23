/**
 * Order Service Tests - Saga Pattern verification
 */
import { OrderServiceImpl } from '../application/OrderServiceImpl';
import { OrderRepository } from '../infrastructure/persistence/OrderRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { OrderStatus, SagaStep, SagaStatus } from '../domain/models/Order';

jest.mock('../infrastructure/persistence/OrderRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');

// Mock global fetch
global.fetch = jest.fn();

describe('OrderServiceImpl', () => {
  let service: OrderServiceImpl;
  let mockRepo: jest.Mocked<OrderRepository>;
  let mockKafka: jest.Mocked<KafkaProducer>;

  beforeEach(() => {
    mockRepo = new OrderRepository() as jest.Mocked<OrderRepository>;
    mockKafka = new KafkaProducer() as jest.Mocked<KafkaProducer>;
    service = new OrderServiceImpl(mockRepo, mockKafka);
    jest.clearAllMocks();
  });

  describe('getOrderById', () => {
    it('should return order when found', async () => {
      const mockOrder = {
        id: 'order-1', userId: 'user-1', status: OrderStatus.PENDING,
        totalAmount: 100, createdAt: new Date(), updatedAt: new Date(),
      };
      mockRepo.findById.mockResolvedValue(mockOrder);

      const result = await service.getOrderById('order-1');
      expect(result).toEqual(mockOrder);
    });

    it('should return null when not found', async () => {
      mockRepo.findById.mockResolvedValue(null);
      const result = await service.getOrderById('nonexistent');
      expect(result).toBeNull();
    });
  });

  describe('cancelOrder', () => {
    it('should cancel a pending order', async () => {
      const mockOrder = {
        id: 'order-1', userId: 'user-1', status: OrderStatus.PENDING,
        totalAmount: 100, createdAt: new Date(), updatedAt: new Date(),
      };
      mockRepo.findById.mockResolvedValue(mockOrder);
      mockRepo.updateStatus.mockResolvedValue({ ...mockOrder, status: OrderStatus.CANCELLED });
      mockKafka.publish.mockResolvedValue();

      const result = await service.cancelOrder('order-1');
      expect(result?.status).toBe(OrderStatus.CANCELLED);
      expect(mockKafka.publish).toHaveBeenCalledWith('order-events', expect.objectContaining({ type: 'ORDER_CANCELLED' }));
    });

    it('should throw error when cancelling confirmed order', async () => {
      mockRepo.findById.mockResolvedValue({
        id: 'order-1', userId: 'user-1', status: OrderStatus.CONFIRMED,
        totalAmount: 100, createdAt: new Date(), updatedAt: new Date(),
      });

      await expect(service.cancelOrder('order-1')).rejects.toThrow('Cannot cancel confirmed order');
    });

    it('should return null for non-existent order', async () => {
      mockRepo.findById.mockResolvedValue(null);
      const result = await service.cancelOrder('nonexistent');
      expect(result).toBeNull();
    });
  });

  describe('createOrder - Saga flow', () => {
    it('should fail if cart is empty', async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: [], total: 0 }),
      });

      await expect(service.createOrder({
        userId: 'user-1', cartId: 'cart-1', paymentMethod: 'CREDIT_CARD',
      })).rejects.toThrow('Cart is empty');
    });

    it('should fail if cart service is unavailable', async () => {
      (global.fetch as jest.Mock).mockRejectedValueOnce(new Error('Connection refused'));

      await expect(service.createOrder({
        userId: 'user-1', cartId: 'cart-1', paymentMethod: 'CREDIT_CARD',
      })).rejects.toThrow('Failed to validate cart');
    });
  });
});
