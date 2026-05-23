/**
 * @layer Use Cases - Error Definitions
 * @description Application-specific errors thrown by use cases.
 * These are distinct from entity validation errors and represent
 * application-level business rule violations.
 */

export class ProductNotFoundError extends Error {
  constructor(id: string) {
    super(`Product with id '${id}' not found`);
    this.name = 'ProductNotFoundError';
  }
}

export class DuplicateSkuError extends Error {
  constructor(sku: string) {
    super(`Product with SKU '${sku}' already exists`);
    this.name = 'DuplicateSkuError';
  }
}

export class InsufficientStockError extends Error {
  constructor(available: number, requested: number) {
    super(`Insufficient stock. Available: ${available}, requested: ${requested}`);
    this.name = 'InsufficientStockError';
  }
}

export class InvalidInputError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'InvalidInputError';
  }
}
