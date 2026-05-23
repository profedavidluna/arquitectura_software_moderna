using Xunit;
using Moq;

namespace CartService.Tests;

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using CartService.Application;
using CartService.Domain.Models;
using CartService.Infrastructure.Messaging;
using CartService.Infrastructure.Persistence;
using CartService.Infrastructure.Persistence.Repositories;
using Moq;

public class CartServiceTests
{
    private readonly CartServiceImpl _service;
    private readonly AppDbContext _context;

    public CartServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        _context = new AppDbContext(options);
        var repository = new CartRepository(_context);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var kafka = new KafkaProducer(config, NullLogger<KafkaProducer>.Instance);
        var httpFactory = new Mock<IHttpClientFactory>();
        httpFactory.Setup(f => f.CreateClient(It.IsAny<string>())).Returns(new HttpClient());
        _service = new CartServiceImpl(repository, httpFactory.Object, kafka, NullLogger<CartServiceImpl>.Instance);
    }

    [Fact]
    public async Task GetOrCreateCart_ShouldCreateNewCart()
    {
        var userId = Guid.NewGuid();
        var cart = await _service.GetOrCreateCartAsync(userId);

        Assert.NotEqual(Guid.Empty, cart.Id);
        Assert.Equal(userId, cart.UserId);
        Assert.Equal("active", cart.Status);
        Assert.Empty(cart.Items);
    }

    [Fact]
    public async Task GetOrCreateCart_ShouldReturnExistingCart()
    {
        var userId = Guid.NewGuid();
        var first = await _service.GetOrCreateCartAsync(userId);
        var second = await _service.GetOrCreateCartAsync(userId);

        Assert.Equal(first.Id, second.Id);
    }

    [Fact]
    public async Task AddItemToCart_ShouldAddItem()
    {
        var userId = Guid.NewGuid();
        var request = new AddItemRequest(Guid.NewGuid(), 2);

        var cart = await _service.AddItemToCartAsync(userId, request);

        Assert.Single(cart.Items);
        Assert.Equal(2, cart.Items[0].Quantity);
    }

    [Fact]
    public async Task ApplyCoupon_ValidCode_ShouldApplyDiscount()
    {
        var userId = Guid.NewGuid();
        await _service.GetOrCreateCartAsync(userId);

        var cart = await _service.ApplyCouponAsync(userId, new ApplyCouponRequest("SAVE10"));

        Assert.Equal("SAVE10", cart.CouponCode);
        Assert.Equal(10m, cart.DiscountPercent);
    }

    [Fact]
    public async Task ApplyCoupon_InvalidCode_ShouldThrow()
    {
        var userId = Guid.NewGuid();
        await _service.GetOrCreateCartAsync(userId);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.ApplyCouponAsync(userId, new ApplyCouponRequest("INVALID")));
    }
}

