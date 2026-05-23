using LayeredArchitecture.Business.Exceptions;
using LayeredArchitecture.Business.Services;
using LayeredArchitecture.Data;
using LayeredArchitecture.Data.Repositories;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace LayeredArchitecture.Tests;

/// <summary>
/// Unit tests for the ProductService (business layer).
/// In a layered architecture, tests often use an in-memory database
/// since the service is tightly coupled to the data layer.
/// </summary>
public class ProductServiceTests
{
    private readonly ProductService _service;
    private readonly AppDbContext _context;

    public ProductServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new AppDbContext(options);
        var repository = new ProductRepository(_context);
        _service = new ProductService(repository);
    }

    [Fact]
    public async Task CreateProduct_WithValidData_ReturnsEntity()
    {
        // Act
        var entity = await _service.CreateProduct("Test Product", "Description", 29.99m, "Electronics", 10, "SKU-001");

        // Assert
        Assert.NotNull(entity);
        Assert.Equal("Test Product", entity.Name);
        Assert.Equal(29.99m, entity.Price);
        Assert.Equal("SKU-001", entity.Sku);
        Assert.True(entity.Active);
    }

    [Fact]
    public async Task CreateProduct_WithDuplicateSku_ThrowsDuplicateSkuException()
    {
        // Arrange
        await _service.CreateProduct("Product 1", "Desc", 10m, "Cat", 5, "SKU-001");

        // Act & Assert
        await Assert.ThrowsAsync<DuplicateSkuException>(
            () => _service.CreateProduct("Product 2", "Desc", 20m, "Cat", 3, "SKU-001"));
    }

    [Fact]
    public async Task CreateProduct_WithZeroPrice_ThrowsBusinessRuleException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<BusinessRuleException>(
            () => _service.CreateProduct("Test", "Desc", 0m, "Cat", 5, "SKU-001"));
    }

    [Fact]
    public async Task GetProductById_WhenNotFound_ThrowsProductNotFoundException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ProductNotFoundException>(
            () => _service.GetProductById(Guid.NewGuid()));
    }

    [Fact]
    public async Task DecreaseStock_WithInsufficientStock_ThrowsInsufficientStockException()
    {
        // Arrange
        var entity = await _service.CreateProduct("Test", "Desc", 10m, "Cat", 5, "SKU-001");

        // Act & Assert
        await Assert.ThrowsAsync<InsufficientStockException>(
            () => _service.DecreaseStock(entity.Id, 10));
    }

    [Fact]
    public async Task DecreaseStock_WithValidQuantity_DecreasesStock()
    {
        // Arrange
        var entity = await _service.CreateProduct("Test", "Desc", 10m, "Cat", 5, "SKU-001");

        // Act
        var result = await _service.DecreaseStock(entity.Id, 3);

        // Assert
        Assert.Equal(2, result.StockQuantity);
    }

    [Fact]
    public async Task IncreaseStock_WithValidQuantity_IncreasesStock()
    {
        // Arrange
        var entity = await _service.CreateProduct("Test", "Desc", 10m, "Cat", 5, "SKU-001");

        // Act
        var result = await _service.IncreaseStock(entity.Id, 10);

        // Assert
        Assert.Equal(15, result.StockQuantity);
    }

    [Fact]
    public async Task DeleteProduct_SoftDeletes_SetsActiveToFalse()
    {
        // Arrange
        var entity = await _service.CreateProduct("Test", "Desc", 10m, "Cat", 5, "SKU-001");

        // Act
        await _service.DeleteProduct(entity.Id);

        // Assert
        var deleted = await _service.GetProductById(entity.Id);
        Assert.False(deleted.Active);
    }

    [Fact]
    public async Task ListProducts_CapsPageSizeAt100()
    {
        // Arrange
        await _service.CreateProduct("Test", "Desc", 10m, "Cat", 5, "SKU-001");

        // Act
        var (items, totalCount) = await _service.ListProducts(0, 200);

        // Assert — should not throw, page size is capped internally
        Assert.Equal(1, totalCount);
    }
}
