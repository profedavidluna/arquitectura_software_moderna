namespace OrderService.Application;

using OrderService.Domain.Interfaces;
using OrderService.Domain.Models;
using OrderService.Infrastructure.Messaging;

public class OrderServiceImpl : IOrderService
{
    private readonly IOrderRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<OrderServiceImpl> _logger;

    public OrderServiceImpl(IOrderRepository repository, KafkaProducer kafkaProducer, ILogger<OrderServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    public async Task<Order?> GetOrderByIdAsync(Guid id) => await _repository.GetByIdAsync(id);

    public async Task<IEnumerable<Order>> GetOrdersByUserIdAsync(Guid userId) => await _repository.GetByUserIdAsync(userId);

    public async Task<IEnumerable<Order>> GetAllOrdersAsync() => await _repository.GetAllAsync();

    public async Task<Order> CreateOrderAsync(CreateOrderRequest request)
    {
        var order = new Order(
            Id: Guid.NewGuid(),
            UserId: request.UserId,
            Status: OrderStatuses.Pending,
            TotalAmount: request.Items.Sum(i => i.UnitPrice * i.Quantity),
            ShippingAddress: request.ShippingAddress,
            SagaState: SagaStates.Initiated,
            Items: request.Items.Select(i => new OrderItem(
                Guid.NewGuid(), Guid.Empty, i.ProductId, i.ProductName, i.UnitPrice, i.Quantity
            )).ToList(),
            CreatedAt: DateTime.UtcNow,
            UpdatedAt: DateTime.UtcNow
        );

        var created = await _repository.CreateAsync(order);

        // Start Saga: Request inventory reservation
        await _kafkaProducer.PublishAsync("order-events", new
        {
            EventType = "OrderCreated",
            OrderId = created.Id,
            UserId = created.UserId,
            Items = created.Items.Select(i => new { i.ProductId, i.Quantity }),
            TotalAmount = created.TotalAmount,
            Timestamp = DateTime.UtcNow
        });

        _logger.LogInformation("Order created, saga initiated: {OrderId}", created.Id);
        return created;
    }

    public async Task<Order> UpdateOrderStatusAsync(Guid id, string status)
    {
        var order = await _repository.GetByIdAsync(id)
            ?? throw new KeyNotFoundException($"Order {id} not found");

        return await _repository.UpdateStatusAsync(id, status, order.SagaState);
    }

    public async Task<Order> AdvanceSagaAsync(Guid orderId, string sagaState)
    {
        var order = await _repository.GetByIdAsync(orderId)
            ?? throw new KeyNotFoundException($"Order {orderId} not found");

        var newStatus = sagaState switch
        {
            SagaStates.InventoryReserved => OrderStatuses.Pending,
            SagaStates.PaymentProcessed => OrderStatuses.Pending,
            SagaStates.Confirmed => OrderStatuses.Confirmed,
            _ => order.Status
        };

        var updated = await _repository.UpdateStatusAsync(orderId, newStatus, sagaState);

        // Publish next saga step
        switch (sagaState)
        {
            case SagaStates.InventoryReserved:
                await _kafkaProducer.PublishAsync("payment-commands", new
                {
                    EventType = "ProcessPayment",
                    OrderId = orderId,
                    UserId = order.UserId,
                    Amount = order.TotalAmount,
                    Timestamp = DateTime.UtcNow
                });
                break;

            case SagaStates.PaymentProcessed:
                await _kafkaProducer.PublishAsync("order-events", new
                {
                    EventType = "OrderConfirmed",
                    OrderId = orderId,
                    UserId = order.UserId,
                    Timestamp = DateTime.UtcNow
                });
                await _repository.UpdateStatusAsync(orderId, OrderStatuses.Confirmed, SagaStates.Confirmed);
                break;
        }

        _logger.LogInformation("Saga advanced for order {OrderId}: {SagaState}", orderId, sagaState);
        return updated;
    }

    public async Task<Order> CompensateSagaAsync(Guid orderId, string reason)
    {
        _logger.LogWarning("Compensating saga for order {OrderId}: {Reason}", orderId, reason);

        var order = await _repository.GetByIdAsync(orderId)
            ?? throw new KeyNotFoundException($"Order {orderId} not found");

        // Publish compensation events based on current saga state
        if (order.SagaState == SagaStates.PaymentProcessed || order.SagaState == SagaStates.InventoryReserved)
        {
            await _kafkaProducer.PublishAsync("inventory-commands", new
            {
                EventType = "ReleaseReservation",
                OrderId = orderId,
                Items = order.Items.Select(i => new { i.ProductId, i.Quantity }),
                Timestamp = DateTime.UtcNow
            });
        }

        if (order.SagaState == SagaStates.PaymentProcessed)
        {
            await _kafkaProducer.PublishAsync("payment-commands", new
            {
                EventType = "RefundPayment",
                OrderId = orderId,
                Amount = order.TotalAmount,
                Timestamp = DateTime.UtcNow
            });
        }

        return await _repository.UpdateStatusAsync(orderId, OrderStatuses.Cancelled, SagaStates.Cancelled);
    }
}
