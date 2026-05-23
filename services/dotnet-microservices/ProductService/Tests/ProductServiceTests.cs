using Xunit;
using Moq;

namespace ProductService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using ProductService.Application;
using ProductService.Domain.Models;
using ProductService.Infrastructure.Cache;
using ProductService.Infrastructure.Messaging;
using ProductService.Infrastructure.Persistence;
using ProductService.Infrastructure.Persistence.Repositories;

public class ProductServiceTests
{
    private readonly ProductServiceImpl _service;

    public ProductServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new AppDbContext(options);
        var repository = new ProductRepository(context);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var cache = new RedisCacheService(config, NullLogger<RedisCacheService>.Instance);
        var kafka = new KafkaProducer(config, NullLogger<KafkaProducer>.Instance);
        _service = new ProductServiceImpl(repository, cache, kafka, NullLogger<ProductServiceImpl>.Instance);
    }

    [Fact]
    public async Task CreateProduct_ShouldReturnProduct()
    {
        var request = new CreateProductRequest("Laptop", "Gaming laptop", 999.99m, null, null);
        var result = await _service.CreateProductAsync(request);

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal("Laptop", result.Name);
        Assert.Equal(999.99m, result.Price);
        Assert.True(result.IsActive);
    }

    [Fact]
    public async Task GetProductById_ShouldReturnProduct()
    {
        var request = new CreateProductRequest("Mouse", "Wireless mouse", 29.99m, null, null);
        var created = await _service.CreateProductAsync(request);

        var found = await _service.GetProductByIdAsync(created.Id);

        Assert.NotNull(found);
        Assert.Equal("Mouse", found.Name);
    }

    [Fact]
    public async Task SearchProducts_ShouldFindByName()
    {
        await _service.CreateProductAsync(new CreateProductRequest("Keyboard Pro", "Mechanical", 79.99m, null, null));
        await _service.CreateProductAsync(new CreateProductRequest("Monitor", "4K Display", 399.99m, null, null));

        var results = await _service.SearchProductsAsync("Keyboard");

        Assert.Single(results);
        Assert.Equal("Keyboard Pro", results.First().Name);
    }

    [Fact]
    public async Task DeleteProduct_ShouldRemoveProduct()
    {
        var created = await _service.CreateProductAsync(new CreateProductRequest("Temp", "Temp item", 10m, null, null));
        var deleted = await _service.DeleteProductAsync(created.Id);

        Assert.True(deleted);
    }

    [Fact]
    public async Task CreateCategory_ShouldReturnCategory()
    {
        var result = await _service.CreateCategoryAsync(new CreateCategoryRequest("Electronics", "Electronic devices", null));

        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal("Electronics", result.Name);
    }
}

