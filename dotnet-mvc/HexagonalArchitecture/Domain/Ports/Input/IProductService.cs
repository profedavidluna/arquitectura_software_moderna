using HexagonalArchitecture.Domain.Models;

namespace HexagonalArchitecture.Domain.Ports.Input;

/// <summary>
/// Input port interface defining the operations available for the Product domain.
/// This belongs to the DOMAIN layer. The domain service implements this interface,
/// and the input adapters (controllers) depend on it.
/// </summary>
public interface IProductService
{
    Task<Product> CreateProduct(string name, string description, decimal price, string category, int stockQuantity, string sku);
    Task<Product> GetProductById(Guid id);
    Task<(IEnumerable<Product> Items, int TotalCount)> ListProducts(int page, int pageSize);
    Task<IEnumerable<Product>> SearchProducts(string query);
    Task<Product> UpdateProduct(Guid id, string name, string description, decimal price, string category, string sku);
    Task DeleteProduct(Guid id);
    Task<Product> DecreaseStock(Guid id, int quantity);
    Task<Product> IncreaseStock(Guid id, int quantity);
}
