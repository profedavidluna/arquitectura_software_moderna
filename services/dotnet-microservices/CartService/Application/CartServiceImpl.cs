namespace CartService.Application;

using System.Text.Json;
using CartService.Domain.Interfaces;
using CartService.Domain.Models;
using CartService.Infrastructure.Messaging;

public class CartServiceImpl : ICartService
{
    private readonly ICartRepository _repository;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<CartServiceImpl> _logger;

    private static readonly Dictionary<string, decimal> Coupons = new()
    {
        { "SAVE10", 10m },
        { "SAVE20", 20m },
        { "WELCOME5", 5m }
    };

    public CartServiceImpl(
        ICartRepository repository,
        IHttpClientFactory httpClientFactory,
        KafkaProducer kafkaProducer,
        ILogger<CartServiceImpl> logger)
    {
        _repository = repository;
        _httpClientFactory = httpClientFactory;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    public async Task<Cart> GetOrCreateCartAsync(Guid userId)
    {
        var cart = await _repository.GetByUserIdAsync(userId);
        if (cart != null) return cart;

        var newCart = new Cart(
            Id: Guid.NewGuid(),
            UserId: userId,
            Status: "active",
            CouponCode: null,
            DiscountPercent: 0,
            Items: new List<CartItem>(),
            CreatedAt: DateTime.UtcNow,
            UpdatedAt: DateTime.UtcNow
        );

        return await _repository.CreateAsync(newCart);
    }

    public async Task<Cart> AddItemToCartAsync(Guid userId, AddItemRequest request)
    {
        var cart = await GetOrCreateCartAsync(userId);

        // Fetch product info from ProductService (with circuit breaker)
        var (productName, unitPrice) = await GetProductInfoAsync(request.ProductId);

        var item = new CartItem(
            Id: Guid.NewGuid(),
            CartId: cart.Id,
            ProductId: request.ProductId,
            ProductName: productName,
            UnitPrice: unitPrice,
            Quantity: request.Quantity,
            CreatedAt: DateTime.UtcNow
        );

        await _repository.AddItemAsync(item);

        await _kafkaProducer.PublishAsync("cart-events", new
        {
            EventType = "ItemAddedToCart",
            UserId = userId,
            ProductId = request.ProductId,
            Quantity = request.Quantity,
            Timestamp = DateTime.UtcNow
        });

        return await _repository.GetByIdAsync(cart.Id) ?? cart;
    }

    public async Task<Cart> UpdateItemQuantityAsync(Guid userId, Guid itemId, UpdateItemQuantityRequest request)
    {
        var cart = await _repository.GetByUserIdAsync(userId)
            ?? throw new KeyNotFoundException("Cart not found");

        var item = await _repository.GetItemAsync(itemId)
            ?? throw new KeyNotFoundException("Item not found");

        var updated = item with { Quantity = request.Quantity };
        await _repository.UpdateItemAsync(updated);

        return await _repository.GetByIdAsync(cart.Id) ?? cart;
    }

    public async Task<Cart> RemoveItemFromCartAsync(Guid userId, Guid itemId)
    {
        var cart = await _repository.GetByUserIdAsync(userId)
            ?? throw new KeyNotFoundException("Cart not found");

        await _repository.RemoveItemAsync(itemId);
        return await _repository.GetByIdAsync(cart.Id) ?? cart;
    }

    public async Task<Cart> ApplyCouponAsync(Guid userId, ApplyCouponRequest request)
    {
        var cart = await _repository.GetByUserIdAsync(userId)
            ?? throw new KeyNotFoundException("Cart not found");

        if (!Coupons.TryGetValue(request.CouponCode.ToUpper(), out var discount))
            throw new InvalidOperationException("Invalid coupon code");

        var updated = cart with
        {
            CouponCode = request.CouponCode.ToUpper(),
            DiscountPercent = discount,
            UpdatedAt = DateTime.UtcNow
        };

        return await _repository.UpdateAsync(updated);
    }

    public async Task<bool> ClearCartAsync(Guid userId)
    {
        var cart = await _repository.GetByUserIdAsync(userId);
        if (cart == null) return false;
        return await _repository.ClearCartAsync(cart.Id);
    }

    private async Task<(string Name, decimal Price)> GetProductInfoAsync(Guid productId)
    {
        try
        {
            var client = _httpClientFactory.CreateClient("ProductService");
            var response = await client.GetAsync($"/api/product/{productId}");

            if (response.IsSuccessStatusCode)
            {
                var json = await response.Content.ReadAsStringAsync();
                var product = JsonSerializer.Deserialize<JsonElement>(json);
                var name = product.GetProperty("name").GetString() ?? "Unknown";
                var price = product.GetProperty("price").GetDecimal();
                return (name, price);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to fetch product {ProductId} from ProductService (circuit breaker may be open)", productId);
        }

        // Fallback when ProductService is unavailable
        return ("Product " + productId.ToString()[..8], 0m);
    }
}
