namespace OrderService.Domain.Models;

/// <summary>
/// Order status enumeration representing the Saga state machine.
/// 
/// Saga Flow:
/// PENDING → STOCK_RESERVED → CONFIRMED (happy path)
/// PENDING → INSUFFICIENT_STOCK → CANCELLED (compensation path)
/// </summary>
public enum OrderStatus
{
    /// <summary>Order created, waiting for stock reservation</summary>
    PENDING,

    /// <summary>Stock has been reserved by Inventory Service</summary>
    STOCK_RESERVED,

    /// <summary>Order confirmed after successful stock reservation</summary>
    CONFIRMED,

    /// <summary>Insufficient stock reported by Inventory Service</summary>
    INSUFFICIENT_STOCK,

    /// <summary>Order cancelled (compensation action in Saga)</summary>
    CANCELLED
}
