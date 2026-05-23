using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moq;
using OrderService.Domain.Models;
using OrderService.Infrastructure.Persistence;
using Xunit;

namespace OrderService.Tests;

/// <summary>
/// Unit tests for Order domain logic and repository operations.
/// Tests focus on business rules without Kafka dependency.
/// </summary>
public class OrderServiceTests : IDisposable
{
    private readonly AppDbContext _context;
    private readonly OrderRepository _repository;

    public OrderServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new AppDbContext(options);
        _repository = new OrderRepository(_context);
    }

    [Fact]
    public async Task CreateOrder_ShouldPersistWithPendingStatus()
    {
        var order = new Order
        {
            CustomerName = "John Doe",
            CustomerEmail = "john@test.com",
            Status = OrderStatus.PENDING,
            TotalAmount = 999.99m,
            Items = new List<OrderItem>
            {
                new() { ProductId = Guid.NewGuid(), ProductName = "Laptop", Quantity = 1, UnitPrice = 999.99m }
            }
        };

        var result = await _repository.CreateAsync(order);

        Assert.NotNull(result);
        Assert.Equal(OrderStatus.PENDING, result.Status);
        Assert.Equal(999.99m, result.TotalAmount);
        Assert.Single(result.Items);
    }

    [Fact]
    public async Task GetAllOrders_ShouldReturnAllOrders()
    {
        await CreateTestOrder("User1");
        await CreateTestOrder("User2");

        var result = await _repository.GetAllAsync();

        Assert.Equal(2, result.Count());
    }

    [Fact]
    public async Task GetOrderById_WhenExists_ShouldReturnOrder()
    {
        var created = await CreateTestOrder("Test User");

        var result = await _repository.GetByIdAsync(created.Id);

        Assert.NotNull(result);
        Assert.Equal(created.Id, result.Id);
    }

    [Fact]
    public async Task UpdateStatus_ShouldChangeOrderStatus()
    {
        var created = await CreateTestOrder("Test User");

        await _repository.UpdateStatusAsync(created.Id, OrderStatus.CONFIRMED);

        var updated = await _repository.GetByIdAsync(created.Id);
        Assert.Equal(OrderStatus.CONFIRMED, updated!.Status);
    }

    [Fact]
    public async Task OrderStatusTransition_PendingToCancelled()
    {
        var created = await CreateTestOrder("Test User");

        await _repository.UpdateStatusAsync(created.Id, OrderStatus.CANCELLED);

        var updated = await _repository.GetByIdAsync(created.Id);
        Assert.Equal(OrderStatus.CANCELLED, updated!.Status);
    }

    private async Task<Order> CreateTestOrder(string customerName)
    {
        var order = new Order
        {
            CustomerName = customerName,
            CustomerEmail = $"{customerName.ToLower().Replace(" ", "")}@test.com",
            Status = OrderStatus.PENDING,
            TotalAmount = 50.00m,
            Items = new List<OrderItem>
            {
                new() { ProductId = Guid.NewGuid(), ProductName = "Item", Quantity = 1, UnitPrice = 50.00m }
            }
        };
        return await _repository.CreateAsync(order);
    }

    public void Dispose() => _context.Dispose();
}
