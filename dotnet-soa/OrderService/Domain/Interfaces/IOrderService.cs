using OrderService.Domain.Models;

namespace OrderService.Domain.Interfaces;

/// <summary>
/// Order service interface following ISP (Interface Segregation Principle).
/// 
/// SOA Principle: Service Contract - defines the operations available
/// for order management including Saga state transitions.
/// </summary>
public interface IOrderService
{
    /// <summary>Creates a new order and initiates the Saga by publishing order.created event.</summary>
    Task<Order> CreateOrderAsync(string customerName, string customerEmail, List<OrderItem> items);

    /// <summary>Retrieves all orders.</summary>
    Task<IEnumerable<Order>> GetAllOrdersAsync();

    /// <summary>Retrieves an order by its unique identifier.</summary>
    Task<Order?> GetOrderByIdAsync(Guid id);

    /// <summary>Handles stock reserved event - Saga step: confirm order.</summary>
    Task HandleStockReservedAsync(Guid orderId);

    /// <summary>Handles insufficient stock event - Saga step: cancel order.</summary>
    Task HandleInsufficientStockAsync(Guid orderId, string reason);
}
