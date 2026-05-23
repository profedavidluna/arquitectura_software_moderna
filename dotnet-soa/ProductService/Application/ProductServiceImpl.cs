using ProductService.Domain.Interfaces;
using ProductService.Domain.Models;
using ProductService.Infrastructure.Messaging;
using ProductService.Infrastructure.Persistence;

namespace ProductService.Application;

/// <summary>
/// Product service implementation following Single Responsibility Principle (SRP).
/// 
/// This class orchestrates product operations and publishes domain events
/// to the Enterprise Service Bus (Kafka) for other services to consume.
/// 
/// Design Pattern: Service Layer - encapsulates business logic and
/// coordinates between persistence and messaging infrastructure.
/// </summary>
public class ProductServiceImpl : IProductService
{
    private readonly ProductRepository _repository;
    private readonly KafkaProducer _kafkaProducer;
    private readonly ILogger<ProductServiceImpl> _logger;

    public ProductServiceImpl(
        ProductRepository repository,
        KafkaProducer kafkaProducer,
        ILogger<ProductServiceImpl> logger)
    {
        _repository = repository;
        _kafkaProducer = kafkaProducer;
        _logger = logger;
    }

    /// <inheritdoc />
    public async Task<Product> CreateProductAsync(string name, string? description, decimal price, string? category)
    {
        var product = new Product
        {
            Name = name,
            Description = description,
            Price = price,
            Category = category
        };

        var created = await _repository.CreateAsync(product);

        // Publish event to ESB (Kafka) - Observer/Pub-Sub pattern
        try
        {
            await _kafkaProducer.PublishAsync("product.created", created.Id.ToString(), new ProductCreatedEvent
            {
                ProductId = created.Id,
                Name = created.Name,
                Description = created.Description,
                Price = created.Price,
                Category = created.Category,
                Timestamp = DateTime.UtcNow
            });
            _logger.LogInformation("Published product.created event for product {ProductId}", created.Id);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to publish product.created event for product {ProductId}", created.Id);
        }

        return created;
    }

    /// <inheritdoc />
    public async Task<IEnumerable<Product>> GetAllProductsAsync()
    {
        return await _repository.GetAllAsync();
    }

    /// <inheritdoc />
    public async Task<Product?> GetProductByIdAsync(Guid id)
    {
        return await _repository.GetByIdAsync(id);
    }

    /// <inheritdoc />
    public async Task<bool> DeleteProductAsync(Guid id)
    {
        return await _repository.DeleteAsync(id);
    }
}
