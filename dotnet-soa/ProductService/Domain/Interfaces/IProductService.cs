using ProductService.Domain.Models;

namespace ProductService.Domain.Interfaces;

/// <summary>
/// Product service interface following Interface Segregation Principle (ISP).
/// 
/// SOA Principle: Service abstraction - consumers interact with the service
/// through a well-defined contract without knowing implementation details.
/// </summary>
public interface IProductService
{
    /// <summary>Creates a new product and publishes a product.created event.</summary>
    Task<Product> CreateProductAsync(string name, string? description, decimal price, string? category);

    /// <summary>Retrieves all products from the catalog.</summary>
    Task<IEnumerable<Product>> GetAllProductsAsync();

    /// <summary>Retrieves a product by its unique identifier.</summary>
    Task<Product?> GetProductByIdAsync(Guid id);

    /// <summary>Deletes a product from the catalog.</summary>
    Task<bool> DeleteProductAsync(Guid id);
}
