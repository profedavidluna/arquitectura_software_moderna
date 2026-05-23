namespace ProductService.Application;

using ProductService.Domain.Interfaces;
using ProductService.Domain.Models;
using ProductService.Infrastructure.Cache;
using ProductService.Infrastructure.Messaging;

public class ProductServiceImpl : IProductService
{
    private readonly IProductRepository _repository;
    private readonly IRedisCacheService _cache;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<ProductServiceImpl> _logger;

    public ProductServiceImpl(
        IProductRepository repository,
        IRedisCacheService cache,
        KafkaProducer kafkaProducer,
        ILogger<ProductServiceImpl> logger)
    {
        _repository = repository;
        _cache = cache;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    public async Task<Product?> GetProductByIdAsync(Guid id)
    {
        var cacheKey = $"product:{id}";
        var cached = await _cache.GetAsync<Product>(cacheKey);
        if (cached != null) return cached;

        var product = await _repository.GetByIdAsync(id);
        if (product != null)
        {
            await _cache.SetAsync(cacheKey, product, TimeSpan.FromMinutes(10));
        }
        return product;
    }

    public async Task<IEnumerable<Product>> GetAllProductsAsync() => await _repository.GetAllAsync();

    public async Task<IEnumerable<Product>> SearchProductsAsync(string query) => await _repository.SearchAsync(query);

    public async Task<Product> CreateProductAsync(CreateProductRequest request)
    {
        var product = new Product(
            Id: Guid.NewGuid(),
            Name: request.Name,
            Description: request.Description,
            Price: request.Price,
            CategoryId: request.CategoryId,
            ImageUrl: request.ImageUrl,
            IsActive: true,
            CreatedAt: DateTime.UtcNow,
            UpdatedAt: DateTime.UtcNow
        );

        var created = await _repository.CreateAsync(product);

        await _kafkaProducer.PublishAsync("product-events", new
        {
            EventType = "ProductCreated",
            ProductId = created.Id,
            Name = created.Name,
            Price = created.Price,
            Timestamp = DateTime.UtcNow
        });

        _logger.LogInformation("Product created: {ProductId}", created.Id);
        return created;
    }

    public async Task<Product> UpdateProductAsync(Guid id, UpdateProductRequest request)
    {
        var product = await _repository.GetByIdAsync(id)
            ?? throw new KeyNotFoundException($"Product {id} not found");

        var updated = product with
        {
            Name = request.Name ?? product.Name,
            Description = request.Description ?? product.Description,
            Price = request.Price ?? product.Price,
            CategoryId = request.CategoryId ?? product.CategoryId,
            ImageUrl = request.ImageUrl ?? product.ImageUrl,
            IsActive = request.IsActive ?? product.IsActive,
            UpdatedAt = DateTime.UtcNow
        };

        var result = await _repository.UpdateAsync(updated);
        await _cache.RemoveAsync($"product:{id}");

        return result;
    }

    public async Task<bool> DeleteProductAsync(Guid id)
    {
        var deleted = await _repository.DeleteAsync(id);
        if (deleted) await _cache.RemoveAsync($"product:{id}");
        return deleted;
    }

    public async Task<Category> CreateCategoryAsync(CreateCategoryRequest request)
    {
        var category = new Category(Guid.NewGuid(), request.Name, request.Description, request.ParentId, DateTime.UtcNow);
        return await _repository.CreateCategoryAsync(category);
    }

    public async Task<IEnumerable<Category>> GetAllCategoriesAsync() => await _repository.GetAllCategoriesAsync();

    public async Task<Review> AddReviewAsync(Guid productId, CreateReviewRequest request)
    {
        _ = await _repository.GetByIdAsync(productId)
            ?? throw new KeyNotFoundException($"Product {productId} not found");

        var review = new Review(Guid.NewGuid(), productId, request.UserId, request.Rating, request.Comment, DateTime.UtcNow);
        return await _repository.AddReviewAsync(review);
    }

    public async Task<IEnumerable<Review>> GetProductReviewsAsync(Guid productId) =>
        await _repository.GetReviewsByProductAsync(productId);
}
