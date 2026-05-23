namespace OrderService.Domain.Interfaces;

using OrderService.Domain.Models;

public interface IOrderRepository
{
    Task<Order?> GetByIdAsync(Guid id);
    Task<IEnumerable<Order>> GetByUserIdAsync(Guid userId);
    Task<IEnumerable<Order>> GetAllAsync();
    Task<Order> CreateAsync(Order order);
    Task<Order> UpdateStatusAsync(Guid id, string status, string sagaState);
    Task<bool> DeleteAsync(Guid id);
}

public interface IOrderService
{
    Task<Order?> GetOrderByIdAsync(Guid id);
    Task<IEnumerable<Order>> GetOrdersByUserIdAsync(Guid userId);
    Task<IEnumerable<Order>> GetAllOrdersAsync();
    Task<Order> CreateOrderAsync(CreateOrderRequest request);
    Task<Order> UpdateOrderStatusAsync(Guid id, string status);
    Task<Order> AdvanceSagaAsync(Guid orderId, string sagaState);
    Task<Order> CompensateSagaAsync(Guid orderId, string reason);
}
