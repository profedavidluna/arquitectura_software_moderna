/**
 * Payment Service Implementation
 * 
 * Demonstrates Retry with Exponential Backoff pattern:
 * When a payment provider call fails (network issue, timeout),
 * we retry with increasing delays: 1s, 2s, 4s, 8s...
 * 
 * This handles transient failures without overwhelming the provider.
 * Combined with a maximum retry count to prevent infinite loops.
 */
import { v4 as uuidv4 } from 'uuid';
import { Payment, PaymentStatus, PaymentMethod, Refund, RefundStatus } from '../domain/models/Payment';
import { IPaymentService, ProcessPaymentDTO, RefundDTO } from '../domain/interfaces/IPaymentService';
import { PaymentRepository } from '../infrastructure/persistence/PaymentRepository';
import { KafkaProducer } from '../infrastructure/messaging/KafkaProducer';
import { PAYMENT_EVENTS_TOPIC, PaymentEventType } from '../infrastructure/messaging/events';

export class PaymentServiceImpl implements IPaymentService {
  private readonly MAX_RETRIES = 3;
  private readonly BASE_DELAY_MS = 1000;

  constructor(
    private readonly paymentRepository: PaymentRepository,
    private readonly kafkaProducer: KafkaProducer
  ) {}

  async processPayment(dto: ProcessPaymentDTO): Promise<Payment> {
    // Create payment record in PENDING state
    const payment: Omit<Payment, 'createdAt' | 'updatedAt'> = {
      id: uuidv4(),
      orderId: dto.orderId,
      userId: dto.userId,
      amount: dto.amount,
      currency: dto.currency || 'USD',
      method: dto.method as PaymentMethod,
      status: PaymentStatus.PROCESSING,
    };

    const created = await this.paymentRepository.create(payment);

    // Process payment with retry logic
    try {
      const transactionId = await this.processWithRetry(created);

      // Payment successful
      const completed = await this.paymentRepository.updateStatus(
        created.id, PaymentStatus.COMPLETED, transactionId
      );

      // Publish success event
      await this.kafkaProducer.publish(PAYMENT_EVENTS_TOPIC, {
        type: PaymentEventType.PAYMENT_COMPLETED,
        data: {
          id: created.id,
          paymentId: created.id,
          orderId: dto.orderId,
          userId: dto.userId,
          amount: dto.amount,
          transactionId,
        },
      });

      return completed!;
    } catch (error: any) {
      // Payment failed after all retries
      await this.paymentRepository.updateStatus(
        created.id, PaymentStatus.FAILED, undefined, error.message
      );

      await this.kafkaProducer.publish(PAYMENT_EVENTS_TOPIC, {
        type: PaymentEventType.PAYMENT_FAILED,
        data: {
          id: created.id,
          paymentId: created.id,
          orderId: dto.orderId,
          reason: error.message,
        },
      });

      throw new Error(`Payment processing failed: ${error.message}`);
    }
  }

  /**
   * Retry with Exponential Backoff
   * 
   * Delays increase exponentially: 1s, 2s, 4s
   * This prevents thundering herd problems when a provider recovers.
   */
  private async processWithRetry(payment: Payment): Promise<string> {
    let lastError: Error | null = null;

    for (let attempt = 0; attempt <= this.MAX_RETRIES; attempt++) {
      try {
        if (attempt > 0) {
          const delay = this.BASE_DELAY_MS * Math.pow(2, attempt - 1);
          console.log(`[Payment] Retry attempt ${attempt}/${this.MAX_RETRIES}, waiting ${delay}ms`);
          await this.sleep(delay);
        }

        // Simulate payment provider call (Stripe/PayPal)
        const transactionId = await this.callPaymentProvider(payment);
        return transactionId;
      } catch (error: any) {
        lastError = error;
        console.warn(`[Payment] Attempt ${attempt + 1} failed: ${error.message}`);
      }
    }

    throw lastError || new Error('Payment processing failed');
  }

  /**
   * Simulated payment provider integration.
   * In production, this would call Stripe, PayPal, etc.
   * Simulates ~90% success rate for demonstration.
   */
  private async callPaymentProvider(payment: Payment): Promise<string> {
    // Simulate network latency
    await this.sleep(100 + Math.random() * 200);

    // Simulate occasional failures (10% failure rate)
    if (Math.random() < 0.1) {
      throw new Error('Payment provider timeout');
    }

    // Simulate validation failures for specific amounts
    if (payment.amount <= 0) {
      throw new Error('Invalid payment amount');
    }

    // Generate a fake transaction ID (like Stripe's pi_xxx)
    const transactionId = `txn_${uuidv4().substring(0, 12)}`;
    console.log(`[Payment] Provider confirmed: ${transactionId} for $${payment.amount}`);
    return transactionId;
  }

  async getPaymentById(id: string): Promise<Payment | null> {
    return this.paymentRepository.findById(id);
  }

  async getPaymentByOrderId(orderId: string): Promise<Payment | null> {
    return this.paymentRepository.findByOrderId(orderId);
  }

  async refundPayment(paymentId: string, dto: RefundDTO): Promise<Refund> {
    const payment = await this.paymentRepository.findById(paymentId);
    if (!payment) throw new Error('Payment not found');
    if (payment.status !== PaymentStatus.COMPLETED) throw new Error('Can only refund completed payments');

    const refundAmount = dto.amount || payment.amount;
    if (refundAmount > payment.amount) throw new Error('Refund amount exceeds payment amount');

    const refund: Omit<Refund, 'createdAt'> = {
      id: uuidv4(),
      paymentId,
      amount: refundAmount,
      reason: dto.reason,
      status: RefundStatus.COMPLETED,
    };

    const created = await this.paymentRepository.createRefund(refund);

    // Update payment status
    await this.paymentRepository.updateStatus(paymentId, PaymentStatus.REFUNDED);

    await this.kafkaProducer.publish(PAYMENT_EVENTS_TOPIC, {
      type: PaymentEventType.PAYMENT_REFUNDED,
      data: {
        id: created.id,
        paymentId,
        orderId: payment.orderId,
        amount: refundAmount,
      },
    });

    return created;
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
