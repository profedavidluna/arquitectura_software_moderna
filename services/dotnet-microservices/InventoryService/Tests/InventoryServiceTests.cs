using Xunit;
using Moq;

namespace InventoryService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using InventoryService.Application;
using InventoryService.Domain.Models;
using InventoryService.Infrastructure.Messaging;
using InventoryService.Infrastructure.Persistence;
using InventoryService.Infrastructure.Persistence.Repositories;

public class InventoryServiceTests
{
    private readonly InventoryServiceImpl _service;

    public InventoryServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new AppDbContext(options);
        var repository = new InventoryRepository(context);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var kafka = new KafkaProducer(config, NullLogger<KafkaProducer>.Instance);
        _service = new InventoryServiceImpl(repository, kafka, NullLogger<InventoryServiceImpl>.Instance);
    }

    [Fact]
    public async Task CreateItem_ShouldReturnInventoryItem()
    {
        var request = new CreateInventoryItemRequest(Guid.NewGuid(), "Laptop", 100, 10);
        var result = await _service.CreateItemAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(100, result.Quantity);
        Assert.Equal(0, result.ReservedQuantity);
        Assert.Equal(100, result.AvailableQuantity);
    }

    [Fact]
    public async Task UpdateStock_ShouldChangeQuantity()
    {
        var productId = Guid.NewGuid();
        await _service.CreateItemAsync(new CreateInventoryItemRequest(productId, "Mouse", 50, 5));

        var result = await _service.UpdateStockAsync(productId, new UpdateStockRequest(25));

        Assert.Equal(75, result.Quantity);
    }

    [Fact]
    public async Task ReserveStock_SufficientStock_ShouldSucceed()
    {
        var productId = Guid.NewGuid();
        await _service.CreateItemAsync(new CreateInventoryItemRequest(productId, "Keyboard", 20, 5));

        var orderId = Guid.NewGuid();
        var success = await _service.ReserveStockAsync(new ReserveStockRequest(orderId, new List<ReserveItemRequest> { new(productId, 5) }));

        Assert.True(success);
        var item = await _service.GetByProductIdAsync(productId);
        Assert.Equal(5, item!.ReservedQuantity);
        Assert.Equal(15, item.AvailableQuantity);
    }

    [Fact]
    public async Task ReserveStock_InsufficientStock_ShouldFail()
    {
        var productId = Guid.NewGuid();
        await _service.CreateItemAsync(new CreateInventoryItemRequest(productId, "Monitor", 3, 5));

        var success = await _service.ReserveStockAsync(new ReserveStockRequest(Guid.NewGuid(), new List<ReserveItemRequest> { new(productId, 10) }));

        Assert.False(success);
    }

    [Fact]
    public async Task ReleaseReservation_ShouldRestoreAvailability()
    {
        var productId = Guid.NewGuid();
        await _service.CreateItemAsync(new CreateInventoryItemRequest(productId, "Headset", 30, 5));

        var orderId = Guid.NewGuid();
        await _service.ReserveStockAsync(new ReserveStockRequest(orderId, new List<ReserveItemRequest> { new(productId, 10) }));
        await _service.ReleaseReservationAsync(orderId);

        var item = await _service.GetByProductIdAsync(productId);
        Assert.Equal(0, item!.ReservedQuantity);
        Assert.Equal(30, item.AvailableQuantity);
    }
}

