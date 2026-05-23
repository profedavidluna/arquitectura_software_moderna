using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moq;
using ProductService.Application;
using ProductService.Domain.Models;
using ProductService.Infrastructure.Messaging;
using ProductService.Infrastructure.Persistence;
using Xunit;

namespace ProductService.Tests;

/// <summary>
/// Unit tests for ProductServiceImpl using xUnit.
/// Uses InMemory EF Core database for repository and mocked Kafka producer.
/// </summary>
public class ProductServiceTests : IDisposable
{
    private readonly AppDbContext _context;
    private readonly ProductRepository _repository;
    private readonly Mock<KafkaProducer> _mockKafkaProducer;
    private readonly Mock<ILogger<ProductServiceImpl>> _mockLogger;
    private readonly ProductServiceImpl _service;

    public ProductServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        _context = new AppDbContext(options);
        _repository = new ProductRepository(_context);
        _mockKafkaProducer = new Mock<KafkaProducer>("localhost:9095") { CallBase = false };
        _mockKafkaProducer.Setup(k => k.PublishAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<object>()))
            .Returns(Task.CompletedTask);
        _mockLogger = new Mock<ILogger<ProductServiceImpl>>();

        _service = new ProductServiceImpl(_repository, _mockKafkaProducer.Object, _mockLogger.Object);
    }

    [Fact]
    public async Task CreateProductAsync_ShouldReturnCreatedProduct()
    {
        var result = await _service.CreateProductAsync("Laptop", "Gaming laptop", 999.99m, "Electronics");

        Assert.NotNull(result);
        Assert.Equal("Laptop", result.Name);
        Assert.Equal(999.99m, result.Price);
        Assert.NotEqual(Guid.Empty, result.Id);
    }

    [Fact]
    public async Task GetAllProductsAsync_ShouldReturnAllProducts()
    {
        await _service.CreateProductAsync("Product 1", "Desc 1", 10.00m, "Cat1");
        await _service.CreateProductAsync("Product 2", "Desc 2", 20.00m, "Cat2");

        var result = await _service.GetAllProductsAsync();

        Assert.Equal(2, result.Count());
    }

    [Fact]
    public async Task GetProductByIdAsync_WhenExists_ShouldReturnProduct()
    {
        var created = await _service.CreateProductAsync("Found", "Desc", 15.00m, "Cat");

        var result = await _service.GetProductByIdAsync(created.Id);

        Assert.NotNull(result);
        Assert.Equal(created.Id, result.Id);
        Assert.Equal("Found", result.Name);
    }

    [Fact]
    public async Task GetProductByIdAsync_WhenNotExists_ShouldReturnNull()
    {
        var result = await _service.GetProductByIdAsync(Guid.NewGuid());

        Assert.Null(result);
    }

    [Fact]
    public async Task DeleteProductAsync_WhenExists_ShouldReturnTrue()
    {
        var created = await _service.CreateProductAsync("ToDelete", "Desc", 5.00m, "Cat");

        var result = await _service.DeleteProductAsync(created.Id);

        Assert.True(result);
        var deleted = await _service.GetProductByIdAsync(created.Id);
        Assert.Null(deleted);
    }

    public void Dispose()
    {
        _context.Dispose();
    }
}
