/**
 * @layer Domain Model
 * @description Pure domain entity representing a Product in the catalog.
 * This class has ZERO framework dependencies - it belongs to the innermost
 * hexagon and contains only business logic and state.
 *
 * In Hexagonal Architecture, the domain model is completely isolated from
 * infrastructure concerns (databases, web frameworks, etc.)
 */
export interface ProductProps {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  stockQuantity: number;
  sku: string;
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export class Product {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly price: number;
  readonly category: string;
  readonly stockQuantity: number;
  readonly sku: string;
  readonly active: boolean;
  readonly createdAt: Date;
  readonly updatedAt: Date;

  constructor(props: ProductProps) {
    this.id = props.id;
    this.name = props.name;
    this.description = props.description;
    this.price = props.price;
    this.category = props.category;
    this.stockQuantity = props.stockQuantity;
    this.sku = props.sku;
    this.active = props.active;
    this.createdAt = props.createdAt;
    this.updatedAt = props.updatedAt;
  }

  /**
   * Business rule: Price must be greater than zero
   */
  static validatePrice(price: number): void {
    if (price <= 0) {
      throw new Error('Price must be greater than 0');
    }
  }

  /**
   * Business rule: Stock cannot go negative
   */
  static validateStock(quantity: number): void {
    if (quantity < 0) {
      throw new Error('Stock quantity cannot be negative');
    }
  }

  /**
   * Decrease stock by a given quantity.
   * Business rule: resulting stock cannot be negative.
   */
  decreaseStock(quantity: number): Product {
    const newStock = this.stockQuantity - quantity;
    if (newStock < 0) {
      throw new Error(`Insufficient stock. Available: ${this.stockQuantity}, requested: ${quantity}`);
    }
    return new Product({
      ...this.toProps(),
      stockQuantity: newStock,
      updatedAt: new Date(),
    });
  }

  /**
   * Increase stock by a given quantity.
   */
  increaseStock(quantity: number): Product {
    if (quantity <= 0) {
      throw new Error('Quantity to increase must be greater than 0');
    }
    return new Product({
      ...this.toProps(),
      stockQuantity: this.stockQuantity + quantity,
      updatedAt: new Date(),
    });
  }

  /**
   * Soft delete: sets active to false.
   */
  deactivate(): Product {
    return new Product({
      ...this.toProps(),
      active: false,
      updatedAt: new Date(),
    });
  }

  toProps(): ProductProps {
    return {
      id: this.id,
      name: this.name,
      description: this.description,
      price: this.price,
      category: this.category,
      stockQuantity: this.stockQuantity,
      sku: this.sku,
      active: this.active,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    };
  }
}
