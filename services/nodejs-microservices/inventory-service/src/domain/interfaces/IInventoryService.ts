import { InventoryItem, Reservation } from '../models/Inventory';

export interface CreateInventoryDTO {
  productId: string;
  quantity: number;
  reorderLevel?: number;
}

export interface ReserveStockDTO {
  orderId: string;
  quantity: number;
}

export interface IInventoryService {
  createInventory(dto: CreateInventoryDTO): Promise<InventoryItem>;
  getByProductId(productId: string): Promise<InventoryItem | null>;
  updateStock(productId: string, quantity: number): Promise<InventoryItem | null>;
  reserveStock(productId: string, dto: ReserveStockDTO): Promise<Reservation>;
  releaseStock(productId: string, orderId: string): Promise<boolean>;
  confirmReservation(orderId: string): Promise<boolean>;
  listAll(): Promise<InventoryItem[]>;
}
