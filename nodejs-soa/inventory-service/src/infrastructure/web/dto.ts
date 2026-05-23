export interface CreateInventoryRequest {
  productId: string;
  productName: string;
  quantityAvailable: number;
}

export interface InventoryResponse {
  id: string;
  productId: string;
  productName: string;
  quantityAvailable: number;
  quantityReserved: number;
  updatedAt: string;
}
