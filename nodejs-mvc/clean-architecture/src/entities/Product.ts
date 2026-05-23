/**
 * @layer Entities (Innermost Layer)
 * @description Enterprise Business Rules - Pure domain entity.
 * In Clean Architecture, entities encapsulate the most general and high-level
 * business rules. They are the least likely to change when something external changes.
 *
 * Entities have ZERO dependencies on any other layer.
 * They contain business logic that would exist even if there were no application.
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
  private constructor(private readonly props: ProductProps) {}

  // Factory method with validation
  static create(props: ProductProps): Product {
    Product.validatePrice(props.price);
    Product.validateStock(props.stockQuantity);
    Product.validateName(props.name);
    Product.validateSku(props.sku);
    return new Product({ ...props });
  }

  // Reconstitute from persistence (no validation - data is already valid)
  static reconstitute(props: ProductProps): Product {
    return new Product({ ...props });
  }

  // --- Business Rules ---

  static validatePrice(price: number): void {
    if (price <= 0) throw new Error('Price must be greater than 0');
  }

  static validateStock(quantity: number): void {
    if (quantity < 0) throw new Error('Stock quantity cannot be negative');
  }

  static validateName(name: string): void {
    if (!name || name.trim().length === 0) throw new Error('Product name is required');
    if (name.length > 255) throw new Error('Product name must be 255 characters or less');
  }

  static validateSku(sku: string): void {
    if (!sku || sku.trim().length === 0) throw new Error('SKU is required');
  }

  decreaseStock(quantity: number): Product {
    if (quantity <= 0) throw new Error('Quantity must be greater than 0');
    const newStock = this.props.stockQuantity - quantity;
    if (newStock < 0) {
      throw new Error(`Insufficient stock. Available: ${this.props.stockQuantity}, requested: ${quantity}`);
    }
    return new Product({ ...this.props, stockQuantity: newStock, updatedAt: new Date() });
  }

  increaseStock(quantity: number): Product {
    if (quantity <= 0) throw new Error('Quantity must be greater than 0');
    return new Product({ ...this.props, stockQuantity: this.props.stockQuantity + quantity, updatedAt: new Date() });
  }

  deactivate(): Product {
    return new Product({ ...this.props, active: false, updatedAt: new Date() });
  }

  updateDetails(updates: Partial<Pick<ProductProps, 'name' | 'description' | 'price' | 'category' | 'stockQuantity' | 'sku'>>): Product {
    if (updates.price !== undefined) Product.validatePrice(updates.price);
    if (updates.stockQuantity !== undefined) Product.validateStock(updates.stockQuantity);
    if (updates.name !== undefined) Product.validateName(updates.name);
    if (updates.sku !== undefined) Product.validateSku(updates.sku);

    return new Product({
      ...this.props,
      ...updates,
      updatedAt: new Date(),
    });
  }

  // --- Getters (read-only access) ---
  get id(): string { return this.props.id; }
  get name(): string { return this.props.name; }
  get description(): string { return this.props.description; }
  get price(): number { return this.props.price; }
  get category(): string { return this.props.category; }
  get stockQuantity(): number { return this.props.stockQuantity; }
  get sku(): string { return this.props.sku; }
  get active(): boolean { return this.props.active; }
  get createdAt(): Date { return this.props.createdAt; }
  get updatedAt(): Date { return this.props.updatedAt; }

  toProps(): ProductProps {
    return { ...this.props };
  }
}
