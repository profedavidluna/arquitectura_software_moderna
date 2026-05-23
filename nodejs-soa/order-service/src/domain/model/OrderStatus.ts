// =============================================================================
// Order Status - Domain Value Object
// =============================================================================
// Represents the possible states of an order in the Saga lifecycle.
//
// State Machine:
// PENDING → CONFIRMED (when stock is reserved)
// PENDING → CANCELLED (when stock is insufficient)
// CONFIRMED → CANCELLED (when user cancels)
// =============================================================================

export enum OrderStatus {
  /** Order created, waiting for inventory confirmation */
  PENDING = 'PENDING',

  /** Stock reserved successfully, order is confirmed */
  CONFIRMED = 'CONFIRMED',

  /** Order cancelled (insufficient stock or user cancellation) */
  CANCELLED = 'CANCELLED',
}

/**
 * Valid state transitions for the order lifecycle.
 * This enforces the Saga state machine rules.
 */
export const validTransitions: Record<OrderStatus, OrderStatus[]> = {
  [OrderStatus.PENDING]: [OrderStatus.CONFIRMED, OrderStatus.CANCELLED],
  [OrderStatus.CONFIRMED]: [OrderStatus.CANCELLED],
  [OrderStatus.CANCELLED]: [], // Terminal state - no transitions allowed
};

/**
 * Check if a status transition is valid.
 */
export function isValidTransition(from: OrderStatus, to: OrderStatus): boolean {
  return validTransitions[from]?.includes(to) ?? false;
}
