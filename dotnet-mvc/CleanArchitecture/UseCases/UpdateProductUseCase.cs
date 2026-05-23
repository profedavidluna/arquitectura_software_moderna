using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: Update an existing product.
/// This belongs to the USE CASES layer (application business rules).
/// Application rule: SKU must remain unique when updating.
/// </summary>
public class UpdateProductUseCase
{
    private readonly IProductGateway _gateway;

    public UpdateProductUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task<Product> Execute(Guid id, string name, string description, decimal price, string category, string sku)
    {
        var product = await _gateway.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        // Application rule: SKU uniqueness check (excluding current product)
        if (await _gateway.ExistsBySkuAndIdNot(sku, id))
            throw new UseCaseException($"A product with SKU '{sku}' already exists.");

        // Entity enforces enterprise rules (price > 0, name required)
        product.Update(name, description, price, category, sku);

        return await _gateway.Update(product);
    }
}
