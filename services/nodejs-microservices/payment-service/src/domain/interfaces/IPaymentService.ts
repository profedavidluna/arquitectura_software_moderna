import { Payment, Refund } from '../models/Payment';

export interface ProcessPaymentDTO {
  orderId: string;
  userId: string;
  amount: number;
  method: string;
  currency?: string;
}

export interface RefundDTO {
  reason?: string;
  amount?: number; // Partial refund support
}

export interface IPaymentService {
  processPayment(dto: ProcessPaymentDTO): Promise<Payment>;
  getPaymentById(id: string): Promise<Payment | null>;
  getPaymentByOrderId(orderId: string): Promise<Payment | null>;
  refundPayment(paymentId: string, dto: RefundDTO): Promise<Refund>;
}
