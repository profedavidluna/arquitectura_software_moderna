import { PaymentServiceImpl } from '../application/PaymentServiceImpl';
import { PaymentRepository } from '../infrastructure/persistence/PaymentRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { PaymentStatus, PaymentMethod, RefundStatus } from '../domain/models/Payment';

jest.mock('../infrastructure/persistence/PaymentRepository');
jest.mock('../infrastructure/messaging/KafkaProducer');

describe('PaymentServiceImpl', () => {
  let service: PaymentServiceImpl;
  let mockRepo: jest.Mocked<PaymentRepository>;
  let mockKafka: jest.Mocked<KafkaProducer>;

  beforeEach(() => {
    mockRepo = new PaymentRepository() as jest.Mocked<PaymentRepository>;
    mockKafka = new KafkaProducer() as jest.Mocked<KafkaProducer>;
    service = new PaymentServiceImpl(mockRepo, mockKafka);
  });

  afterEach(() => jest.clearAllMocks());

  describe('processPayment', () => {
    it('should process payment successfully', async () => {
      mockRepo.create.mockResolvedValue({
        id: 'pay-1', orderId: 'order-1', userId: 'user-1',
        amount: 99.99, currency: 'USD', method: PaymentMethod.CREDIT_CARD,
        status: PaymentStatus.PROCESSING, createdAt: new Date(), updatedAt: new Date(),
      });
      mockRepo.updateStatus.mockResolvedValue({
        id: 'pay-1', orderId: 'order-1', userId: 'user-1',
        amount: 99.99, currency: 'USD', method: PaymentMethod.CREDIT_CARD,
        status: PaymentStatus.COMPLETED, providerTransactionId: 'txn_123',
        createdAt: new Date(), updatedAt: new Date(),
      });
      mockKafka.publish.mockResolvedValue();

      const result = await service.processPayment({
        orderId: 'order-1', userId: 'user-1', amount: 99.99, method: 'CREDIT_CARD',
      });

      expect(result.status).toBe(PaymentStatus.COMPLETED);
      expect(mockKafka.publish).toHaveBeenCalledWith('payment-events', expect.objectContaining({ type: 'PAYMENT_COMPLETED' }));
    });
  });

  describe('refundPayment', () => {
    it('should refund a completed payment', async () => {
      mockRepo.findById.mockResolvedValue({
        id: 'pay-1', orderId: 'order-1', userId: 'user-1',
        amount: 99.99, currency: 'USD', method: PaymentMethod.CREDIT_CARD,
        status: PaymentStatus.COMPLETED, createdAt: new Date(), updatedAt: new Date(),
      });
      mockRepo.createRefund.mockResolvedValue({
        id: 'ref-1', paymentId: 'pay-1', amount: 99.99,
        reason: 'Customer request', status: RefundStatus.COMPLETED, createdAt: new Date(),
      });
      mockRepo.updateStatus.mockResolvedValue(null);
      mockKafka.publish.mockResolvedValue();

      const result = await service.refundPayment('pay-1', { reason: 'Customer request' });

      expect(result.status).toBe(RefundStatus.COMPLETED);
      expect(result.amount).toBe(99.99);
    });

    it('should reject refund for non-completed payment', async () => {
      mockRepo.findById.mockResolvedValue({
        id: 'pay-1', orderId: 'order-1', userId: 'user-1',
        amount: 99.99, currency: 'USD', method: PaymentMethod.CREDIT_CARD,
        status: PaymentStatus.PENDING, createdAt: new Date(), updatedAt: new Date(),
      });

      await expect(service.refundPayment('pay-1', {})).rejects.toThrow('Can only refund completed payments');
    });

    it('should reject refund exceeding payment amount', async () => {
      mockRepo.findById.mockResolvedValue({
        id: 'pay-1', orderId: 'order-1', userId: 'user-1',
        amount: 50, currency: 'USD', method: PaymentMethod.CREDIT_CARD,
        status: PaymentStatus.COMPLETED, createdAt: new Date(), updatedAt: new Date(),
      });

      await expect(service.refundPayment('pay-1', { amount: 100 })).rejects.toThrow('Refund amount exceeds payment amount');
    });
  });

  describe('getPaymentById', () => {
    it('should return payment when found', async () => {
      const payment = {
        id: 'pay-1', orderId: 'order-1', userId: 'user-1',
        amount: 99.99, currency: 'USD', method: PaymentMethod.CREDIT_CARD,
        status: PaymentStatus.COMPLETED, createdAt: new Date(), updatedAt: new Date(),
      };
      mockRepo.findById.mockResolvedValue(payment);

      const result = await service.getPaymentById('pay-1');
      expect(result).toEqual(payment);
    });
  });
});
