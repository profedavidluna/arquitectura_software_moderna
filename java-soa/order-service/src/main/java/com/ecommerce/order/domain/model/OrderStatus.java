package com.ecommerce.order.domain.model;

/**
 * Order Status Enum - Domain Layer
 * 
 * <p><b>Saga Pattern - State Machine</b>:
 * This enum represents the possible states in the order saga.
 * Each transition is triggered by an event from the ESB.</p>
 * 
 * <pre>
 * State Transitions:
 * PENDING → CONFIRMED (when stock.reserved received)
 * PENDING → CANCELLED (when stock.insufficient received)
 * CONFIRMED → SHIPPED (future: when shipping confirms)
 * CONFIRMED → CANCELLED (when user cancels)
 * </pre>
 */
public enum OrderStatus {

    /** Order created, waiting for inventory confirmation */
    PENDING,

    /** Stock reserved, order confirmed */
    CONFIRMED,

    /** Order cancelled due to insufficient stock or user request */
    CANCELLED,

    /** Order shipped to customer */
    SHIPPED,

    /** Order delivered to customer */
    DELIVERED
}
