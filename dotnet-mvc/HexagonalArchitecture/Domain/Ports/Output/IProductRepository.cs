using HexagonalArchitecture.Domain.Models;

namespace HexagonalArchitecture.Domain.Ports.Output;

/// <summary>
/// Output port interface defining the persistence operations for Products.
/// This belongs to the DOMAIN layer. The output adapters (repositories) implement this interface.
/// The domain service depends on this abstraction, NOT on concrete implementations.
/// </summary>
public interface IProductRepository
{
    Task<Product> Save(Product product);
    Task<Product?> FindById(Guid id);
    Task<(IEnumerable<Product> Items, int TotalCount)> FindAll(int page, int pageSize);
    Task<IEnumerable<Product>> Search(string query);
    Task<bool> ExistsBySku(string sku);
    Task<bool> ExistsBySkuAndIdNot(string sku, Guid id);
    Task<Product> Update(Product product);
    Task Delete(Guid id);
}
