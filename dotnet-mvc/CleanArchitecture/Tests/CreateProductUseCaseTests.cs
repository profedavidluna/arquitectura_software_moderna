using CleanArchitecture.Entities;
using CleanArchitecture.UseCases;
using CleanArchitecture.UseCases.Ports;
using Moq;
using Xunit;

namespace CleanArchitecture.Tests;

/// <summary>
/// Unit tests for the CreateProductUseCase.
/// Demonstrates how Clean Architecture enables testing use cases in isolation.
/// The gateway (outer layer) is mocked — use cases don't depend on infrastructure.
/// </summary>
public class CreateProductUseCaseTests
{
    private readonly Mock<IProductGateway> _gatewayMock;
    private readonly CreateProductUseCase _useCase;

    public CreateProductUseCaseTests()
    {
        _gatewayMock = new Mock<IProductGateway>();
        _useCase = new CreateProductUseCase(_gatewayMock.Object);
    }

    [Fact]
    public async Task Execute_WithValidData_CreatesAndReturnsProduct()
    {
        // Arrange
        _gatewayMock.Setup(g => g.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);
        _gatewayMock.Setup(g => g.Save(It.IsAny<Product>()))
            .ReturnsAsync((Product p) => p);

        // Act
        var product = await _useCase.Execute("Test Product", "Description", 29.99m, "Electronics", 10, "SKU-001");

        // Assert
        Assert.NotNull(product);
        Assert.Equal("Test Product", product.Name);
        Assert.Equal(29.99m, product.Price);
        Assert.Equal("SKU-001", product.Sku);
        Assert.Equal(10, product.StockQuantity);
        Assert.True(product.Active);
        _gatewayMock.Verify(g => g.Save(It.IsAny<Product>()), Times.Once);
    }

    [Fact]
    public async Task Execute_WithDuplicateSku_ThrowsUseCaseException()
    {
        // Arrange
        _gatewayMock.Setup(g => g.ExistsBySku("SKU-001")).ReturnsAsync(true);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<UseCaseException>(
            () => _useCase.Execute("Test", "Desc", 10m, "Cat", 5, "SKU-001"));

        Assert.Contains("SKU", exception.Message);
    }

    [Fact]
    public async Task Execute_WithZeroPrice_ThrowsEntityValidationException()
    {
        // Arrange
        _gatewayMock.Setup(g => g.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);

        // Act & Assert
        // Entity enforces enterprise rule: price > 0
        await Assert.ThrowsAsync<EntityValidationException>(
            () => _useCase.Execute("Test", "Desc", 0m, "Cat", 5, "SKU-001"));
    }

    [Fact]
    public async Task Execute_WithNegativePrice_ThrowsEntityValidationException()
    {
        // Arrange
        _gatewayMock.Setup(g => g.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);

        // Act & Assert
        await Assert.ThrowsAsync<EntityValidationException>(
            () => _useCase.Execute("Test", "Desc", -5m, "Cat", 5, "SKU-001"));
    }

    [Fact]
    public async Task Execute_WithEmptyName_ThrowsEntityValidationException()
    {
        // Arrange
        _gatewayMock.Setup(g => g.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);

        // Act & Assert
        await Assert.ThrowsAsync<EntityValidationException>(
            () => _useCase.Execute("", "Desc", 10m, "Cat", 5, "SKU-001"));
    }

    [Fact]
    public async Task Execute_WithNegativeStock_ThrowsEntityValidationException()
    {
        // Arrange
        _gatewayMock.Setup(g => g.ExistsBySku(It.IsAny<string>())).ReturnsAsync(false);

        // Act & Assert
        await Assert.ThrowsAsync<EntityValidationException>(
            () => _useCase.Execute("Test", "Desc", 10m, "Cat", -1, "SKU-001"));
    }
}

/// <summary>
/// Unit tests for the ManageStockUseCase.
/// </summary>
public class ManageStockUseCaseTests
{
    private readonly Mock<IProductGateway> _gatewayMock;
    private readonly ManageStockUseCase _useCase;

    public ManageStockUseCaseTests()
    {
        _gatewayMock = new Mock<IProductGateway>();
        _useCase = new ManageStockUseCase(_gatewayMock.Object);
    }

    [Fact]
    public async Task DecreaseStock_WithValidQuantity_DecreasesStock()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _gatewayMock.Setup(g => g.FindById(product.Id)).ReturnsAsync(product);
        _gatewayMock.Setup(g => g.Update(It.IsAny<Product>())).ReturnsAsync((Product p) => p);

        // Act
        var result = await _useCase.DecreaseStock(product.Id, 3);

        // Assert
        Assert.Equal(2, result.StockQuantity);
    }

    [Fact]
    public async Task DecreaseStock_WithInsufficientStock_ThrowsEntityValidationException()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _gatewayMock.Setup(g => g.FindById(product.Id)).ReturnsAsync(product);

        // Act & Assert
        await Assert.ThrowsAsync<EntityValidationException>(
            () => _useCase.DecreaseStock(product.Id, 10));
    }

    [Fact]
    public async Task DecreaseStock_WhenProductNotFound_ThrowsProductNotFoundException()
    {
        // Arrange
        var id = Guid.NewGuid();
        _gatewayMock.Setup(g => g.FindById(id)).ReturnsAsync((Product?)null);

        // Act & Assert
        await Assert.ThrowsAsync<ProductNotFoundException>(
            () => _useCase.DecreaseStock(id, 3));
    }

    [Fact]
    public async Task IncreaseStock_WithValidQuantity_IncreasesStock()
    {
        // Arrange
        var product = Product.Create("Test", "Desc", 10m, "Cat", 5, "SKU-001");
        _gatewayMock.Setup(g => g.FindById(product.Id)).ReturnsAsync(product);
        _gatewayMock.Setup(g => g.Update(It.IsAny<Product>())).ReturnsAsync((Product p) => p);

        // Act
        var result = await _useCase.IncreaseStock(product.Id, 10);

        // Assert
        Assert.Equal(15, result.StockQuantity);
    }
}
