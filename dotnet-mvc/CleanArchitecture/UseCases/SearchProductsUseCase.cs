using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: Search products by query string.
/// This belongs to the USE CASES layer (application business rules).
/// </summary>
public class SearchProductsUseCase
{
    private readonly IProductGateway _gateway;

    public SearchProductsUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task<IEnumerable<Product>> Execute(string query)
    {
        if (string.IsNullOrWhiteSpace(query))
            return Enumerable.Empty<Product>();

        return await _gateway.Search(query);
    }
}
