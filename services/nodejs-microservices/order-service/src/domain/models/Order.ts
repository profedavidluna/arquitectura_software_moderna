export interface Order {
  id: string;
  userId: string;
  status: OrderStatus;
  totalAmount: number;
  shippingAddressId?: string;
  paymentId?: string;
  notes?: string;
  createdAt: Date;
  updatedAt: Date;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  INVENTORY_RESERVED = 'INVENTORY_RESERVED',
  PAYMENT_PROCESSING = 'PAYMENT_PROCESSING',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED',
  FAILED = 'FAILED',
}

export interface OrderItem {
  id: string;
  orderId: string;
  productId: string;
  productName?: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/**
 * Saga State tracks the progress of a distributed transaction.
 * If any step fails, we use compensation data to rollback previous steps.
 */
export interface SagaState {
  id: string;
  orderId: string;
  currentStep: SagaStep;
  status: SagaStatus;
  compensationData?: any;
  createdAt: Date;
  updatedAt: Date;
}

export enum SagaStep {
  VALIDATE_CART = 'VALIDATE_CART',
  RESERVE_INVENTORY = 'RESERVE_INVENTORY',
  PROCESS_PAYMENT = 'PROCESS_PAYMENT',
  CONFIRM_ORDER = 'CONFIRM_ORDER',
  COMPLETED = 'COMPLETED',
}

export enum SagaStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  COMPENSATING = 'COMPENSATING',
  FAILED = 'FAILED',
}
