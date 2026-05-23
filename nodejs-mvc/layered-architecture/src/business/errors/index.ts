/**
 * @layer Business Layer - Custom Errors
 * @description Custom error classes for the business layer.
 * These provide semantic meaning to errors and help the presentation
 * layer determine appropriate HTTP status codes.
 */

export class NotFoundError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'NotFoundError';
  }
}

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ValidationError';
  }
}

export class ConflictError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ConflictError';
  }
}

export class InsufficientStockError extends Error {
  constructor(available: number, requested: number) {
    super(`Insufficient stock. Available: ${available}, requested: ${requested}`);
    this.name = 'InsufficientStockError';
  }
}
