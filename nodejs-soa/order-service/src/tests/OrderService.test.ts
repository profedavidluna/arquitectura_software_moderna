// =============================================================================
// Order Service - Unit Tests
// =============================================================================
// Tests the Saga orchestration logic with mocked dependencies.
// Verifies correct state transitions and event publishing.
// =============================================================================

import { OrderService } from '../application/OrderService';
import { OrderRepository } from '../infrastructure/persistence/OrderRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { Order, CreateOrderData } from '../domain/model/Order';
import { OrderStatus } from '../domain/model/OrderStatus';

jest.mock('../infrastructure/persistence/OrderRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');

describe('OrderService', () => {
  let orderService: OrderService;
  let mockRepository: jest.Mocked<OrderRepository>;
  let mockKafkaProducer: jest.Mocked<KafkaProducer>;

  const mockOrder: Order = {
    id: 'order-123',
    userId: 'user-456',
    status: OrderStatus.PENDING,
    totalAmount: 2599.98,
    items: [
      {
        id: 'item-1',
        orderId: 'order-123',
        productId: 'product-789',
        productName: 'Laptop Pro 15',
        quantity: 2,
        unitPrice: 1299.99,
        subtotal: 2599.98,
      },
    ],
    createdAt: new Date('2024-01-01'),
    updatedAt: new Date('2024-01-01'),
  };

  beforeEach(() => {
    mockRepository = new OrderRepository(null as any) as jest.Mocked<OrderRepository>;
    mockKafkaProducer = new KafkaProducer([], '') as jest.Mocked<KafkaProducer>;
    orderService = new OrderService(mockRepository, mockKafkaProducer);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('create (Saga Step 1)', () => {
    const createData: CreateOrderData = {
      userId: 'user-456',
      items: [
        {
          productId: 'product-789',
          productName: 'Laptop Pro 15',
          quantity: 2,
          unitPrice: 1299.99,
        },
      ],
    };

    it('should create order in PENDING state and publish event', async () => {
      mockRepository.create = jest.fn().mockResolvedValue(mockOrder);
      mockKafkaProducer.publish = jest.fn().mockResolvedValue(undefined);

      const result = await orderService.create(createData);

      expect(result.status).toBe(OrderStatus.PENDING);
      expect(mockRepository.create).toHaveBeenCalledWith(createData);
      expect(mockKafkaProducer.publish).toHaveBeenCalledWith(
        'order.created',
        mockOrder.id,
        expect.objectContaining({
          eventType: 'ORDER_CREATED',
          data: expect.objectContaining({
            orderId: mockOrder.id,
            userId: mockOrder.userId,
          }),
        })
      );
    });

    it('should create order even if Kafka fails', async () => {
      mockRepository.create = jest.fn().mockResolvedValue(mockOrder);
      mockKafkaProducer.publish = jest.fn().mockRejectedValue(new Error('Kafka down'));

      const result = await orderService.create(createData);

      expect(result).toEqual(mockOrder);
    });
  });

  describe('confirm (Saga Step 3 - Happy Path)', () => {
    it('should confirm a PENDING order', async () => {
      const confirmedOrder = { ...mockOrder, status: OrderStatus.CONFIRMED };
      mockRepository.findById = jest.fn().mockResolvedValue(mockOrder);
      mockRepository.updateStatus = jest.fn().mockResolvedValue(confirmedOrder);
      mockKafkaProducer.publish = jest.fn().mockResolvedValue(undefined);

      const result = await orderService.confirm('order-123');

      expect(result?.status).toBe(OrderStatus.CONFIRMED);
      expect(mockRepository.updateStatus).toHaveBeenCalledWith('order-123', OrderStatus.CONFIRMED);
      expect(mockKafkaProducer.publish).toHaveBeenCalledWith(
        'order.confirmed',
        'order-123',
        expect.objectContaining({ eventType: 'ORDER_CONFIRMED' })
      );
    });

    it('should not confirm an already CANCELLED order', async () => {
      const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };
      mockRepository.findById = jest.fn().mockResolvedValue(cancelledOrder);

      const result = await orderService.confirm('order-123');

      expect(result?.status).toBe(OrderStatus.CANCELLED);
      expect(mockRepository.updateStatus).not.toHaveBeenCalled();
    });
  });

  describe('cancel (Compensating Transaction)', () => {
    it('should cancel a PENDING order and publish event', async () => {
      const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };
      mockRepository.findById = jest.fn().mockResolvedValue(mockOrder);
      mockRepository.updateStatus = jest.fn().mockResolvedValue(cancelledOrder);
      mockKafkaProducer.publish = jest.fn().mockResolvedValue(undefined);

      const result = await orderService.cancel('order-123');

      expect(result?.status).toBe(OrderStatus.CANCELLED);
      expect(mockKafkaProducer.publish).toHaveBeenCalledWith(
        'order.cancelled',
        'order-123',
        expect.objectContaining({ eventType: 'ORDER_CANCELLED' })
      );
    });

    it('should cancel a CONFIRMED order and publish event', async () => {
      const confirmedOrder = { ...mockOrder, status: OrderStatus.CONFIRMED };
      const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };
      mockRepository.findById = jest.fn().mockResolvedValue(confirmedOrder);
      mockRepository.updateStatus = jest.fn().mockResolvedValue(cancelledOrder);
      mockKafkaProducer.publish = jest.fn().mockResolvedValue(undefined);

      const result = await orderService.cancel('order-123');

      expect(result?.status).toBe(OrderStatus.CANCELLED);
    });

    it('should return null when order not found', async () => {
      mockRepository.findById = jest.fn().mockResolvedValue(null);

      const result = await orderService.cancel('non-existent');

      expect(result).toBeNull();
    });
  });

  describe('markInsufficientStock (Saga Step 3 - Failure Path)', () => {
    it('should cancel order due to insufficient stock', async () => {
      const cancelledOrder = { ...mockOrder, status: OrderStatus.CANCELLED };
      mockRepository.findById = jest.fn().mockResolvedValue(mockOrder);
      mockRepository.updateStatus = jest.fn().mockResolvedValue(cancelledOrder);

      const result = await orderService.markInsufficientStock('order-123');

      expect(result?.status).toBe(OrderStatus.CANCELLED);
      expect(mockRepository.updateStatus).toHaveBeenCalledWith('order-123', OrderStatus.CANCELLED);
    });
  });
});
