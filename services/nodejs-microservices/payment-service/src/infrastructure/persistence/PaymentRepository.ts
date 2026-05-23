import { pool } from './database';
import { Payment, PaymentStatus, PaymentMethod, Refund, RefundStatus } from '../../domain/models/Payment';

export class PaymentRepository {
  async create(payment: Omit<Payment, 'createdAt' | 'updatedAt'>): Promise<Payment> {
    const query = `
      INSERT INTO payments (id, order_id, user_id, amount, currency, method, status, provider_transaction_id, failure_reason)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING *
    `;
    const result = await pool.query(query, [
      payment.id, payment.orderId, payment.userId, payment.amount,
      payment.currency, payment.method, payment.status,
      payment.providerTransactionId, payment.failureReason,
    ]);
    return this.mapToPayment(result.rows[0]);
  }

  async findById(id: string): Promise<Payment | null> {
    const result = await pool.query('SELECT * FROM payments WHERE id = $1', [id]);
    return result.rows.length > 0 ? this.mapToPayment(result.rows[0]) : null;
  }

  async findByOrderId(orderId: string): Promise<Payment | null> {
    const result = await pool.query('SELECT * FROM payments WHERE order_id = $1 ORDER BY created_at DESC LIMIT 1', [orderId]);
    return result.rows.length > 0 ? this.mapToPayment(result.rows[0]) : null;
  }

  async updateStatus(id: string, status: PaymentStatus, transactionId?: string, failureReason?: string): Promise<Payment | null> {
    const query = `
      UPDATE payments SET status = $1, provider_transaction_id = COALESCE($2, provider_transaction_id),
      failure_reason = COALESCE($3, failure_reason), updated_at = CURRENT_TIMESTAMP
      WHERE id = $4 RETURNING *
    `;
    const result = await pool.query(query, [status, transactionId, failureReason, id]);
    return result.rows.length > 0 ? this.mapToPayment(result.rows[0]) : null;
  }

  async createRefund(refund: Omit<Refund, 'createdAt'>): Promise<Refund> {
    const query = `
      INSERT INTO refunds (id, payment_id, amount, reason, status)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING *
    `;
    const result = await pool.query(query, [refund.id, refund.paymentId, refund.amount, refund.reason, refund.status]);
    return this.mapToRefund(result.rows[0]);
  }

  private mapToPayment(row: any): Payment {
    return {
      id: row.id, orderId: row.order_id, userId: row.user_id,
      amount: parseFloat(row.amount), currency: row.currency,
      method: row.method as PaymentMethod, status: row.status as PaymentStatus,
      providerTransactionId: row.provider_transaction_id,
      failureReason: row.failure_reason,
      createdAt: row.created_at, updatedAt: row.updated_at,
    };
  }

  private mapToRefund(row: any): Refund {
    return {
      id: row.id, paymentId: row.payment_id, amount: parseFloat(row.amount),
      reason: row.reason, status: row.status as RefundStatus, createdAt: row.created_at,
    };
  }
}
