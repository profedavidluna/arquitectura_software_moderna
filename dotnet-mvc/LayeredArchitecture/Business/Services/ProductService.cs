using LayeredArchitecture.Business.Exceptions;
using LayeredArchitecture.Data.Entities;
using LayeredArchitecture.Data.Repositories;

namespace LayeredArchitecture.Business.Services;

/// <summary>
/// Service containing business logic for Product operations.
/// This belongs to the BUSINESS LAYER. In a layered architecture, the service
/// directly depends on the Data layer (repository and entities).
/// Dependencies flow downward: Presentation → Business → Data.
/// </summary>
public class ProductService
{
    private readonly ProductRepository _repository;

    public ProductService(ProductRepository repository)
    {
        _repository = repository;
    }

    public async Task<ProductEntity> CreateProduct(string name, string description, decimal price, string category, int stockQuantity, string sku)
    {
        if (price <= 0)
            throw new BusinessRuleException("Price must be greater than zero.");

        if (string.IsNullOrWhiteSpace(name))
            throw new BusinessRuleException("Product name is required.");

        if (string.IsNullOrWhiteSpace(sku))
            throw new BusinessRuleException("SKU is required.");

        if (stockQuantity < 0)
            throw new BusinessRuleException("Stock quantity cannot be negative.");

        if (await _repository.ExistsBySku(sku))
            throw new DuplicateSkuException(sku);

        var entity = new ProductEntity
        {
            Id = Guid.NewGuid(),
            Name = name,
            Description = description ?? string.Empty,
            Price = price,
            Category = category ?? string.Empty,
            StockQuantity = stockQuantity,
            Sku = sku,
            Active = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        return await _repository.Save(entity);
    }

    public async Task<ProductEntity> GetProductById(Guid id)
    {
        var entity = await _repository.FindById(id);
        if (entity is null)
            throw new ProductNotFoundException(id);

        return entity;
    }

    public async Task<(List<ProductEntity> Items, int TotalCount)> ListProducts(int page, int pageSize)
    {
        if (pageSize > 100) pageSize = 100;
        if (pageSize < 1) pageSize = 10;
        if (page < 0) page = 0;

        return await _repository.FindAll(page, pageSize);
    }

    public async Task<List<ProductEntity>> SearchProducts(string query)
    {
        if (string.IsNullOrWhiteSpace(query))
            return new List<ProductEntity>();

        return await _repository.Search(query);
    }

    public async Task<ProductEntity> UpdateProduct(Guid id, string name, string description, decimal price, string category, string sku)
    {
        if (price <= 0)
            throw new BusinessRuleException("Price must be greater than zero.");

        if (string.IsNullOrWhiteSpace(name))
            throw new BusinessRuleException("Product name is required.");

        var entity = await _repository.FindById(id);
        if (entity is null)
            throw new ProductNotFoundException(id);

        if (await _repository.ExistsBySkuAndIdNot(sku, id))
            throw new DuplicateSkuException(sku);

        entity.Name = name;
        entity.Description = description ?? string.Empty;
        entity.Price = price;
        entity.Category = category ?? string.Empty;
        entity.Sku = sku;
        entity.UpdatedAt = DateTime.UtcNow;

        return await _repository.Update(entity);
    }

    public async Task DeleteProduct(Guid id)
    {
        var entity = await _repository.FindById(id);
        if (entity is null)
            throw new ProductNotFoundException(id);

        entity.Active = false;
        entity.UpdatedAt = DateTime.UtcNow;
        await _repository.Update(entity);
    }

    public async Task<ProductEntity> DecreaseStock(Guid id, int quantity)
    {
        if (quantity <= 0)
            throw new BusinessRuleException("Quantity must be greater than zero.");

        var entity = await _repository.FindById(id);
        if (entity is null)
            throw new ProductNotFoundException(id);

        if (entity.StockQuantity - quantity < 0)
            throw new InsufficientStockException();

        entity.StockQuantity -= quantity;
        entity.UpdatedAt = DateTime.UtcNow;
        return await _repository.Update(entity);
    }

    public async Task<ProductEntity> IncreaseStock(Guid id, int quantity)
    {
        if (quantity <= 0)
            throw new BusinessRuleException("Quantity must be greater than zero.");

        var entity = await _repository.FindById(id);
        if (entity is null)
            throw new ProductNotFoundException(id);

        entity.StockQuantity += quantity;
        entity.UpdatedAt = DateTime.UtcNow;
        return await _repository.Update(entity);
    }
}
