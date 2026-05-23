/**
 * Inventory Item Domain Model.
 * Contains business rules for stock reservation and release.
 * 
 * Saga Pattern: Supports reserve (forward) and release (compensate) operations.
 */
export interface InventoryItemProps {
  id: string;
  productId: string;
  productName: string;
  quantityAvailable: number;
  quantityReserved: number;
  updatedAt: Date;
}

export class InventoryItem {
  readonly id: string;
  readonly productId: string;
  readonly productName: string;
  quantityAvailable: number;
  quantityReserved: number;
  updatedAt: Date;

  constructor(props: InventoryItemProps) {
    this.id = props.id;
    this.productId = props.productId;
    this.productName = props.productName;
    this.quantityAvailable = props.quantityAvailable;
    this.quantityReserved = props.quantityReserved;
    this.updatedAt = props.updatedAt;
  }

  hasAvailableStock(quantity: number): boolean {
    return this.quantityAvailable >= quantity;
  }

  /** Saga forward action: reserve stock */
  reserveStock(quantity: number): void {
    if (!this.hasAvailableStock(quantity)) {
      throw new Error(`Insufficient stock. Available: ${this.quantityAvailable}, requested: ${quantity}`);
    }
    this.quantityAvailable -= quantity;
    this.quantityReserved += quantity;
    this.updatedAt = new Date();
  }

  /** Saga compensating action: release reserved stock */
  releaseStock(quantity: number): void {
    if (this.quantityReserved < quantity) {
      throw new Error(`Cannot release ${quantity}. Only ${this.quantityReserved} reserved.`);
    }
    this.quantityReserved -= quantity;
    this.quantityAvailable += quantity;
    this.updatedAt = new Date();
  }

  addStock(quantity: number): void {
    if (quantity <= 0) throw new Error('Quantity must be positive');
    this.quantityAvailable += quantity;
    this.updatedAt = new Date();
  }
}
