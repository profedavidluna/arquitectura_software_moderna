using HexagonalArchitecture.Domain.Models;
using HexagonalArchitecture.Domain.Ports.Output;
using HexagonalArchitecture.Domain.Services;
using Moq;
using Xunit;

namespace HexagonalArchitecture.Tests;

/// <summary>
/// Unit tests for the ProductService (domain service).
/// These tests verify business logic in isolation by mocking the output port.
/// Demonstrates how Hexagonal Architecture enables easy testing of the domain layer.
/// </summary>
public class ProductServiceTests
{
    private readonly Mock<IProductRepository> _repositoryMock;
    private readonly ProductService _service;

    public ProductServiceTests()
    {
        _repositoryMock = new Mock<IProductRepository>();
        _service = new ProductService(_repositoryMock.Object);
    }

    [Fact]
    public async Task CreateProduct_WithValidData_ReturnsProduct()
    {
        // Arrange
        _repositoryMock.Setup(r => r.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);
        _repositoryMock.Setup(r => r.Save(It.IsAny<Product>()))
            .ReturnsAsync((Product p) => p);

        // Act
        var product = await _service.CreateProduct("Test Product", "Description", 29.99m, "Electronics", 10, "SKU-001");

        // Assert
        Assert.NotNull(product);
        Assert.Equal("Test Product", product.Name);
        Assert.Equal(29.99m, product.Price);
        Assert.Equal("SKU-001", product.Sku);
        Assert.True(product.Active);
        _repositoryMock.Verify(r => r.Save(It.IsAny<Product>()), Times.Once);
    }

    [Fact]
    public async Task CreateProduct_WithDuplicateSku_ThrowsDomainException()
    {
        // Arrange
        _repositoryMock.Setup(r => r.ExistsBySku("SKU-001")).ReturnsAsync(true);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<DomainException>(
            () => _service.CreateProduct("Test", "Desc", 10m, "Cat", 5, "SKU-001"));

        Assert.Contains("SKU", exception.Message);
    }

    [Fact]
    public async Task CreateProduct_WithZeroPrice_ThrowsDomainException()
    {
        // Arrange
        _repositoryMock.Setup(r => r.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);

        // Act & Assert
        await Assert.ThrowsAsync<DomainException>(
            () => _service.CreateProduct("Test", "Desc", 0m, "Cat", 5, "SKU-001"));
    }

    [Fact]
    public async Task GetProductById_WhenNotFound_ThrowsProductNotFoundException()
    {
        // Arrange
        var id = Guid.NewGuid();
        _repositoryMock.Setup(r => r.FindById(id)).ReturnsAsync((Product?)null);

        // Act & Assert
        await Assert.ThrowsAsync<ProductNotFoundException>(
            () => _service.GetProductById(id));
    }

    [Fact]
    public async Task DecreaseStock_WithInsufficientStock_ThrowsDomainException()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _repositoryMock.Setup(r => r.FindById(product.Id)).ReturnsAsync(product);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<DomainException>(
            () => _service.DecreaseStock(product.Id, 10));

        Assert.Contains("Insufficient stock", exception.Message);
    }

    [Fact]
    public async Task DecreaseStock_WithValidQuantity_DecreasesStock()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _repositoryMock.Setup(r => r.FindById(product.Id)).ReturnsAsync(product);
        _repositoryMock.Setup(r => r.Update(It.IsAny<Product>())).ReturnsAsync((Product p) => p);

        // Act
        var result = await _service.DecreaseStock(product.Id, 3);

        // Assert
        Assert.Equal(2, result.StockQuantity);
    }

    [Fact]
    public async Task IncreaseStock_WithValidQuantity_IncreasesStock()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _repositoryMock.Setup(r => r.FindById(product.Id)).ReturnsAsync(product);
        _repositoryMock.Setup(r => r.Update(It.IsAny<Product>())).ReturnsAsync((Product p) => p);

        // Act
        var result = await _service.IncreaseStock(product.Id, 10);

        // Assert
        Assert.Equal(15, result.StockQuantity);
    }

    [Fact]
    public async Task DeleteProduct_SoftDeletes_SetsActiveToFalse()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _repositoryMock.Setup(r => r.FindById(product.Id)).ReturnsAsync(product);
        _repositoryMock.Setup(r => r.Update(It.IsAny<Product>())).ReturnsAsync((Product p) => p);

        // Act
        await _service.DeleteProduct(product.Id);

        // Assert
        Assert.False(product.Active);
        _repositoryMock.Verify(r => r.Update(It.Is<Product>(p => !p.Active)), Times.Once);
    }

    [Fact]
    public async Task ListProducts_CapsPageSizeAt100()
    {
        // Arrange
        _repositoryMock.Setup(r => r.FindAll(0, 100))
            .ReturnsAsync((new List<Product>(), 0));

        // Act
        await _service.ListProducts(0, 200);

        // Assert
        _repositoryMock.Verify(r => r.FindAll(0, 100), Times.Once);
    }
}
