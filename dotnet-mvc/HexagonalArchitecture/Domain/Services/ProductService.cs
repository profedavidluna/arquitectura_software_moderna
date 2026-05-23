using HexagonalArchitecture.Domain.Models;
using HexagonalArchitecture.Domain.Ports.Input;
using HexagonalArchitecture.Domain.Ports.Output;

namespace HexagonalArchitecture.Domain.Services;

/// <summary>
/// Domain service implementing the input port.
/// This belongs to the DOMAIN layer and contains application/orchestration logic.
/// It has NO framework annotations — it is registered via DI in Program.cs.
/// It depends only on the output port interface (IProductRepository), not on concrete implementations.
/// </summary>
public class ProductService : IProductService
{
    private readonly IProductRepository _repository;

    public ProductService(IProductRepository repository)
    {
        _repository = repository;
    }

    public async Task<Product> CreateProduct(string name, string description, decimal price, string category, int stockQuantity, string sku)
    {
        if (await _repository.ExistsBySku(sku))
            throw new DomainException($"A product with SKU '{sku}' already exists.");

        var product = Product.Create(name, description, price, category, stockQuantity, sku);
        return await _repository.Save(product);
    }

    public async Task<Product> GetProductById(Guid id)
    {
        var product = await _repository.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        return product;
    }

    public async Task<(IEnumerable<Product> Items, int TotalCount)> ListProducts(int page, int pageSize)
    {
        if (pageSize > 100) pageSize = 100;
        if (pageSize < 1) pageSize = 10;
        if (page < 0) page = 0;

        return await _repository.FindAll(page, pageSize);
    }

    public async Task<IEnumerable<Product>> SearchProducts(string query)
    {
        if (string.IsNullOrWhiteSpace(query))
            return Enumerable.Empty<Product>();

        return await _repository.Search(query);
    }

    public async Task<Product> UpdateProduct(Guid id, string name, string description, decimal price, string category, string sku)
    {
        var product = await _repository.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        if (await _repository.ExistsBySkuAndIdNot(sku, id))
            throw new DomainException($"A product with SKU '{sku}' already exists.");

        product.Update(name, description, price, category, sku);
        return await _repository.Update(product);
    }

    public async Task DeleteProduct(Guid id)
    {
        var product = await _repository.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        product.Deactivate();
        await _repository.Update(product);
    }

    public async Task<Product> DecreaseStock(Guid id, int quantity)
    {
        var product = await _repository.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        product.DecreaseStock(quantity);
        return await _repository.Update(product);
    }

    public async Task<Product> IncreaseStock(Guid id, int quantity)
    {
        var product = await _repository.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        product.IncreaseStock(quantity);
        return await _repository.Update(product);
    }
}

/// <summary>
/// Exception thrown when a product is not found.
/// </summary>
public class ProductNotFoundException : Exception
{
    public Guid ProductId { get; }

    public ProductNotFoundException(Guid id)
        : base($"Product with ID '{id}' was not found.")
    {
        ProductId = id;
    }
}
