using CleanArchitecture.Entities;

namespace CleanArchitecture.UseCases.Ports;

/// <summary>
/// Gateway interface (port) for product persistence operations.
/// This belongs to the USE CASES layer. It defines what the use cases need
/// from the outer layers, without knowing HOW it's implemented.
/// The Dependency Rule: inner layers define interfaces, outer layers implement them.
/// </summary>
public interface IProductGateway
{
    Task<Product> Save(Product product);
    Task<Product?> FindById(Guid id);
    Task<(IEnumerable<Product> Items, int TotalCount)> FindAll(int page, int pageSize);
    Task<IEnumerable<Product>> Search(string query);
    Task<bool> ExistsBySku(string sku);
    Task<bool> ExistsBySkuAndIdNot(string sku, Guid id);
    Task<Product> Update(Product product);
}
