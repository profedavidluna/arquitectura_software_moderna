using Xunit;
using Moq;

namespace OrderService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using OrderService.Application;
using OrderService.Domain.Models;
using OrderService.Infrastructure.Messaging;
using OrderService.Infrastructure.Persistence;
using OrderService.Infrastructure.Persistence.Repositories;

public class OrderServiceTests
{
    private readonly OrderServiceImpl _service;

    public OrderServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new AppDbContext(options);
        var repository = new OrderRepository(context);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var kafka = new KafkaProducer(config, NullLogger<KafkaProducer>.Instance);
        _service = new OrderServiceImpl(repository, kafka, NullLogger<OrderServiceImpl>.Instance);
    }

    [Fact]
    public async Task CreateOrder_ShouldReturnOrderWithSagaInitiated()
    {
        var request = new CreateOrderRequest(
            Guid.NewGuid(), "123 Main St",
            new List<OrderItemRequest> { new(Guid.NewGuid(), "Laptop", 999.99m, 1) }
        );

        var result = await _service.CreateOrderAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(OrderStatuses.Pending, result.Status);
        Assert.Equal(SagaStates.Initiated, result.SagaState);
        Assert.Equal(999.99m, result.TotalAmount);
        Assert.Single(result.Items);
    }

    [Fact]
    public async Task AdvanceSaga_InventoryReserved_ShouldUpdateState()
    {
        var request = new CreateOrderRequest(
            Guid.NewGuid(), "456 Oak Ave",
            new List<OrderItemRequest> { new(Guid.NewGuid(), "Mouse", 29.99m, 2) }
        );
        var order = await _service.CreateOrderAsync(request);

        var updated = await _service.AdvanceSagaAsync(order.Id, SagaStates.InventoryReserved);

        Assert.Equal(SagaStates.InventoryReserved, updated.SagaState);
    }

    [Fact]
    public async Task CompensateSaga_ShouldCancelOrder()
    {
        var request = new CreateOrderRequest(
            Guid.NewGuid(), "789 Pine Rd",
            new List<OrderItemRequest> { new(Guid.NewGuid(), "Keyboard", 79.99m, 1) }
        );
        var order = await _service.CreateOrderAsync(request);

        var compensated = await _service.CompensateSagaAsync(order.Id, "Payment failed");

        Assert.Equal(OrderStatuses.Cancelled, compensated.Status);
        Assert.Equal(SagaStates.Cancelled, compensated.SagaState);
    }

    [Fact]
    public async Task GetOrdersByUserId_ShouldReturnUserOrders()
    {
        var userId = Guid.NewGuid();
        await _service.CreateOrderAsync(new CreateOrderRequest(userId, "Addr 1", new List<OrderItemRequest> { new(Guid.NewGuid(), "Item1", 10m, 1) }));
        await _service.CreateOrderAsync(new CreateOrderRequest(userId, "Addr 2", new List<OrderItemRequest> { new(Guid.NewGuid(), "Item2", 20m, 1) }));

        var orders = await _service.GetOrdersByUserIdAsync(userId);

        Assert.Equal(2, orders.Count());
    }
}

