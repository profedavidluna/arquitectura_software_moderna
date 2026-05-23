export interface Notification {
  id: string;
  type: NotificationType;
  recipient: string;
  subject: string;
  body: string;
  status: NotificationStatus;
  sentAt?: Date;
  createdAt: Date;
}

export enum NotificationType {
  ORDER_CONFIRMATION = 'ORDER_CONFIRMATION',
  ORDER_CANCELLED = 'ORDER_CANCELLED',
  PAYMENT_RECEIVED = 'PAYMENT_RECEIVED',
  PAYMENT_REFUNDED = 'PAYMENT_REFUNDED',
  WELCOME = 'WELCOME',
  LOW_STOCK = 'LOW_STOCK',
}

export enum NotificationStatus {
  PENDING = 'PENDING',
  SENT = 'SENT',
  FAILED = 'FAILED',
}
