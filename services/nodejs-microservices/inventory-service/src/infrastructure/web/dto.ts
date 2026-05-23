import { InventoryItem, Reservation } from '../../domain/models/Inventory';

export interface CreateInventoryRequest { productId: string; quantity: number; reorderLevel?: number; }
export interface ReserveStockRequest { orderId: string; quantity: number; }
export interface ReleaseStockRequest { orderId: string; quantity: number; }

export function toInventoryResponse(item: InventoryItem) {
  return {
    id: item.id,
    productId: item.productId,
    availableQuantity: item.quantity,
    reservedQuantity: item.reservedQuantity,
    totalQuantity: item.quantity + item.reservedQuantity,
    reorderLevel: item.reorderLevel,
    updatedAt: item.updatedAt.toISOString(),
  };
}

export function toReservationResponse(reservation: Reservation) {
  return {
    id: reservation.id,
    orderId: reservation.orderId,
    productId: reservation.productId,
    quantity: reservation.quantity,
    status: reservation.status,
    expiresAt: reservation.expiresAt?.toISOString(),
    createdAt: reservation.createdAt.toISOString(),
  };
}
