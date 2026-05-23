export interface InventoryItem {
  id: string;
  productId: string;
  quantity: number;
  reservedQuantity: number;
  reorderLevel: number;
  updatedAt: Date;
}

export interface Reservation {
  id: string;
  orderId: string;
  productId: string;
  quantity: number;
  status: ReservationStatus;
  expiresAt?: Date;
  createdAt: Date;
}

export enum ReservationStatus {
  ACTIVE = 'ACTIVE',
  CONFIRMED = 'CONFIRMED',
  RELEASED = 'RELEASED',
  EXPIRED = 'EXPIRED',
}
