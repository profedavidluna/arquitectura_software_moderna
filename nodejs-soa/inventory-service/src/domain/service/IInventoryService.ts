import { InventoryItem } from '../model/InventoryItem';

/**
 * Inventory Service Interface (ISP - Interface Segregation Principle).
 * Defines the contract for inventory operations.
 */
export interface IInventoryService {
  createItem(productId: string, productName: string, quantity: number): Promise<InventoryItem>;
  getByProductId(productId: string): Promise<InventoryItem | null>;
  getAll(): Promise<InventoryItem[]>;
  reserveStock(productId: string, quantity: number, orderId: string): Promise<boolean>;
  releaseStock(productId: string, quantity: number, orderId: string): Promise<void>;
  addStock(productId: string, quantity: number): Promise<InventoryItem>;
}
