using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: Soft-delete a product (set active=false).
/// This belongs to the USE CASES layer (application business rules).
/// Application rule: delete is a soft-delete, not a physical removal.
/// </summary>
public class DeleteProductUseCase
{
    private readonly IProductGateway _gateway;

    public DeleteProductUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task Execute(Guid id)
    {
        var product = await _gateway.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        // Entity handles the deactivation logic
        product.Deactivate();

        await _gateway.Update(product);
    }
}
