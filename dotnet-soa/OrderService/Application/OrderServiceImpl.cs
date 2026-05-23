using OrderService.Domain.Interfaces;
using OrderService.Domain.Models;
using OrderService.Infrastructure.Messaging;
using OrderService.Infrastructure.Persistence;

namespace OrderService.Application;

/// <summary>
/// Order service implementation - Saga Orchestrator.
/// 
/// Design Pattern: Saga - orchestrates a distributed transaction across
/// multiple services (Order + Inventory) using compensating actions.
/// 
/// Saga Flow:
/// 1. CreateOrder → publishes "order.created" → Inventory reserves stock
/// 2a. Stock reserved → publishes "order.confirmed" (happy path)
/// 2b. Insufficient stock → publishes "order.cancelled" → Inventory releases stock
/// </summary>
public class OrderServiceImpl : IOrderService
{
    private readonly OrderRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<OrderServiceImpl> _logger;

    public OrderServiceImpl(
        OrderRepository repository,
        KafkaProducer kafkaProducer,
        ILogger<OrderServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    /// <inheritdoc />
    public async Task<Order> CreateOrderAsync(string customerName, string customerEmail, List<OrderItem> items)
    {
        var totalAmount = items.Sum(i => i.Quantity * i.UnitPrice);

        var order = new Order
        {
            CustomerName = customerName,
            CustomerEmail = customerEmail,
            Status = OrderStatus.PENDING,
            TotalAmount = totalAmount,
            Items = items
        };

        var created = await _repository.CreateAsync(order);

        // Saga Step 1: Publish order.created event to trigger stock reservation
        try
        {
            await _kafkaProducer.PublishAsync("order.created", created.Id.ToString(), new OrderCreatedEvent
            {
                OrderId = created.Id,
                CustomerName = created.CustomerName,
                CustomerEmail = created.CustomerEmail,
                Items = created.Items.Select(i => new OrderItemEvent
                {
                    ProductId = i.ProductId,
                    ProductName = i.ProductName,
                    Quantity = i.Quantity,
                    UnitPrice = i.UnitPrice
                }).ToList(),
                TotalAmount = created.TotalAmount,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Saga initiated: Published order.created for order {OrderId}", created.Id);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish order.created event for order {OrderId}", created.Id);
        }

        return created;
    }

    /// <inheritdoc />
    public async Task<IEnumerable<Order>> GetAllOrdersAsync()
    {
        return await _repository.GetAllAsync();
    }

    /// <inheritdoc />
    public async Task<Order?> GetOrderByIdAsync(Guid id)
    {
        return await _repository.GetByIdAsync(id);
    }

    /// <inheritdoc />
    public async Task HandleStockReservedAsync(Guid orderId)
    {
        _logger.LogInformation("Saga step: Stock reserved for order {OrderId}, confirming order", orderId);

        await _repository.UpdateStatusAsync(orderId, OrderStatus.CONFIRMED);

        // Saga Step 2a: Publish order.confirmed event
        try
        {
            await _kafkaProducer.PublishAsync("order.confirmed", orderId.ToString(), new OrderConfirmedEvent
            {
                OrderId = orderId,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Saga completed: Published order.confirmed for order {OrderId}", orderId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish order.confirmed event for order {OrderId}", orderId);
        }
    }

    /// <inheritdoc />
    public async Task HandleInsufficientStockAsync(Guid orderId, string reason)
    {
        _logger.LogWarning("Saga compensation: Insufficient stock for order {OrderId}. Reason: {Reason}", orderId, reason);

        await _repository.UpdateStatusAsync(orderId, OrderStatus.CANCELLED);

        // Saga Step 2b (Compensation): Publish order.cancelled event
        try
        {
            await _kafkaProducer.PublishAsync("order.cancelled", orderId.ToString(), new OrderCancelledEvent
            {
                OrderId = orderId,
                Reason = reason,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Saga compensated: Published order.cancelled for order {OrderId}", orderId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to publish order.cancelled event for order {OrderId}", orderId);
        }
    }
}
