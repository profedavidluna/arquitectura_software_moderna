using CleanArchitecture.Entities;
using CleanArchitecture.UseCases.Ports;

namespace CleanArchitecture.UseCases;

/// <summary>
/// Use Case: List products with pagination.
/// This belongs to the USE CASES layer (application business rules).
/// Application rule: max page size is 100.
/// </summary>
public class ListProductsUseCase
{
    private readonly IProductGateway _gateway;

    public ListProductsUseCase(IProductGateway gateway)
    {
        _gateway = gateway;
    }

    public async Task<(IEnumerable<Product> Items, int TotalCount)> Execute(int page, int pageSize)
    {
        // Application rule: cap page size at 100
        if (pageSize > 100) pageSize = 100;
        if (pageSize < 1) pageSize = 10;
        if (page < 0) page = 0;

        return await _gateway.FindAll(page, pageSize);
    }
}
