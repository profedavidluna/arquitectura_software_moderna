using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: Manage product stock (increase/decrease).
/// This belongs to the USE CASES layer (application business rules).
/// The entity enforces the enterprise rule that stock cannot go negative.
/// </summary>
public class ManageStockUseCase
{
    private readonly IProductGateway _gateway;

    public ManageStockUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task<Product> DecreaseStock(Guid id, int quantity)
    {
        var product = await _gateway.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        // Entity enforces enterprise rule: stock cannot go below zero
        product.DecreaseStock(quantity);

        return await _gateway.Update(product);
    }

    public async Task<Product> IncreaseStock(Guid id, int quantity)
    {
        var product = await _gateway.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        product.IncreaseStock(quantity);

        return await _gateway.Update(product);
    }
}
