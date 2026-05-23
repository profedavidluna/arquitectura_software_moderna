using Microsoft.EntityFrameworkCore;
using InventoryService.Domain.Models;
using InventoryService.Infrastructure.Persistence;
using Xunit;

namespace InventoryService.Tests;

/// <summary>
/// Unit tests for Inventory repository and domain logic.
/// Uses InMemory EF Core database for isolation.
/// </summary>
public class InventoryServiceTests : IDisposable
{
    private readonly AppDbContext _context;
    private readonly InventoryRepository _repository;

    public InventoryServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new AppDbContext(options);
        _repository = new InventoryRepository(_context);
    }

    [Fact]
    public async Task CreateInventory_ShouldPersistItem()
    {
        var item = new InventoryItem
        {
            ProductId = Guid.NewGuid(),
            ProductName = "Laptop",
            Quantity = 50,
            ReservedQuantity = 0
        };

        var result = await _repository.CreateAsync(item);

        Assert.NotNull(result);
        Assert.Equal("Laptop", result.ProductName);
        Assert.Equal(50, result.Quantity);
    }

    [Fact]
    public async Task GetByProductId_WhenExists_ShouldReturnItem()
    {
        var productId = Guid.NewGuid();
        await _repository.CreateAsync(new InventoryItem
        {
            ProductId = productId, ProductName = "Mouse", Quantity = 100, ReservedQuantity = 0
        });

        var result = await _repository.GetByProductIdAsync(productId);

        Assert.NotNull(result);
        Assert.Equal(productId, result.ProductId);
    }

    [Fact]
    public async Task GetByProductId_WhenNotExists_ShouldReturnNull()
    {
        var result = await _repository.GetByProductIdAsync(Guid.NewGuid());
        Assert.Null(result);
    }

    [Fact]
    public async Task ReserveStock_ShouldIncreaseReservedQuantity()
    {
        var productId = Guid.NewGuid();
        await _repository.CreateAsync(new InventoryItem
        {
            ProductId = productId, ProductName = "Keyboard", Quantity = 30, ReservedQuantity = 0
        });

        await _repository.ReserveStockAsync(productId, 5);

        var updated = await _repository.GetByProductIdAsync(productId);
        Assert.Equal(5, updated!.ReservedQuantity);
    }

    [Fact]
    public async Task ReleaseStock_ShouldDecreaseReservedQuantity()
    {
        var productId = Guid.NewGuid();
        await _repository.CreateAsync(new InventoryItem
        {
            ProductId = productId, ProductName = "Monitor", Quantity = 20, ReservedQuantity = 10
        });

        await _repository.ReleaseStockAsync(productId, 5);

        var updated = await _repository.GetByProductIdAsync(productId);
        Assert.Equal(5, updated!.ReservedQuantity);
    }

    [Fact]
    public async Task GetAll_ShouldReturnAllItems()
    {
        await _repository.CreateAsync(new InventoryItem { ProductId = Guid.NewGuid(), ProductName = "A", Quantity = 10, ReservedQuantity = 0 });
        await _repository.CreateAsync(new InventoryItem { ProductId = Guid.NewGuid(), ProductName = "B", Quantity = 20, ReservedQuantity = 0 });

        var result = await _repository.GetAllAsync();

        Assert.Equal(2, result.Count());
    }

    [Fact]
    public async Task AvailableStock_ShouldBeQuantityMinusReserved()
    {
        var productId = Guid.NewGuid();
        await _repository.CreateAsync(new InventoryItem
        {
            ProductId = productId, ProductName = "Widget", Quantity = 50, ReservedQuantity = 15
        });

        var item = await _repository.GetByProductIdAsync(productId);

        // Available = Quantity - Reserved
        Assert.Equal(35, item!.Quantity - item.ReservedQuantity);
    }

    public void Dispose() => _context.Dispose();
}
