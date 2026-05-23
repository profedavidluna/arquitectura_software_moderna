using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: Get a product by its ID.
/// This belongs to the USE CASES layer (application business rules).
/// Single responsibility: retrieve a product or throw if not found.
/// </summary>
public class GetProductUseCase
{
    private readonly IProductGateway _gateway;

    public GetProductUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task<Product> Execute(Guid id)
    {
        var product = await _gateway.FindById(id);
        if (product is null)
            throw new ProductNotFoundException(id);

        return product;
    }
}
