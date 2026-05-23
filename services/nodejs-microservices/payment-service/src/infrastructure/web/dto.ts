import { Payment, Refund } from '../../domain/models/Payment';

export interface ProcessPaymentRequest {
  orderId: string;
  userId: string;
  amount: number;
  method: string;
  currency?: string;
}

export interface RefundRequest {
  reason?: string;
  amount?: number;
}

export function toPaymentResponse(payment: Payment) {
  return {
    id: payment.id,
    orderId: payment.orderId,
    userId: payment.userId,
    amount: payment.amount,
    currency: payment.currency,
    method: payment.method,
    status: payment.status,
    providerTransactionId: payment.providerTransactionId,
    failureReason: payment.failureReason,
    createdAt: payment.createdAt.toISOString(),
    updatedAt: payment.updatedAt.toISOString(),
  };
}

export function toRefundResponse(refund: Refund) {
  return {
    id: refund.id,
    paymentId: refund.paymentId,
    amount: refund.amount,
    reason: refund.reason,
    status: refund.status,
    createdAt: refund.createdAt.toISOString(),
  };
}
