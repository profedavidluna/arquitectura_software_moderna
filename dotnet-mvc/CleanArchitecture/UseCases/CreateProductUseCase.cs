using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: Create a new product.
/// This belongs to the USE CASES layer (application business rules).
/// Each use case is a single class with a single responsibility.
/// It orchestrates the flow: validates application rules, creates the entity, persists it.
/// </summary>
public class CreateProductUseCase
{
    private readonly IProductGateway _gateway;

    public CreateProductUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task<Product> Execute(string name, string description, decimal price, string category, int stockQuantity, string sku)
    {
        // Application rule: SKU must be unique across the system
        if (await _gateway.ExistsBySku(sku))
            throw new UseCaseException($"A product with SKU '{sku}' already exists.");

        // Entity creation enforces enterprise rules (price > 0, name required, etc.)
        var product = Product.Create(name, description, price, category, stockQuantity, sku);

        return await _gateway.Save(product);
    }
}

/// <summary>
/// Exception for use case (application) rule violations.
/// Distinct from EntityValidationException (enterprise rules).
/// </summary>
public class UseCaseException : Exception
{
    public UseCaseException(string message) : base(message) { }
}

/// <summary>
/// Exception thrown when a product is not found (application rule).
/// </summary>
public class ProductNotFoundException : UseCaseException
{
    public Guid ProductId { get; }

    public ProductNotFoundException(Guid id)
        : base($"Product with ID '{id}' was not found.")
    {
        ProductId = id;
    }
}
