using HexagonalArchitecture.Domain.Models;
using HexagonalArchitecture.Domain.Ports.Output;
using Microsoft.EntityFrameworkCore;

namespace HexagonalArchitecture.Adapters.Output.Persistence;

/// <summary>
/// Output adapter implementing the IProductRepository port.
/// This belongs to the OUTPUT ADAPTER layer. It translates between
/// the domain model (Product) and the persistence model (ProductEntity).
/// </summary>
public class ProductRepositoryAdapter : IProductRepository
{
    private readonly AppDbContext _context;

    public ProductRepositoryAdapter(AppDbContext context)
    {
        _context = context;
    }

    public async Task<Product> Save(Product product)
    {
        var entity = ToEntity(product);
        _context.Products.Add(entity);
        await _context.SaveChangesAsync();
        return product;
    }

    public async Task<Product?> FindById(Guid id)
    {
        var entity = await _context.Products.FindAsync(id);
        return entity is null ? null : ToDomain(entity);
    }

    public async Task<(IEnumerable<Product> Items, int TotalCount)> FindAll(int page, int pageSize)
    {
        var totalCount = await _context.Products.Where(p => p.Active).CountAsync();
        var items = await _context.Products
            .Where(p => p.Active)
            .OrderByDescending(p => p.CreatedAt)
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items.Select(ToDomain), totalCount);
    }

    public async Task<IEnumerable<Product>> Search(string query)
    {
        var lowerQuery = query.ToLower();
        var items = await _context.Products
            .Where(p => p.Active &&
                (p.Name.ToLower().Contains(lowerQuery) ||
                 p.Category.ToLower().Contains(lowerQuery) ||
                 p.Description.ToLower().Contains(lowerQuery)))
            .OrderByDescending(p => p.CreatedAt)
            .Take(100)
            .ToListAsync();

        return items.Select(ToDomain);
    }

    public async Task<bool> ExistsBySku(string sku)
    {
        return await _context.Products.AnyAsync(p => p.Sku == sku);
    }

    public async Task<bool> ExistsBySkuAndIdNot(string sku, Guid id)
    {
        return await _context.Products.AnyAsync(p => p.Sku == sku && p.Id != id);
    }

    public async Task<Product> Update(Product product)
    {
        var entity = await _context.Products.FindAsync(product.Id);
        if (entity is null)
            throw new InvalidOperationException($"Product entity with ID {product.Id} not found.");

        entity.Name = product.Name;
        entity.Description = product.Description;
        entity.Price = product.Price;
        entity.Category = product.Category;
        entity.StockQuantity = product.StockQuantity;
        entity.Sku = product.Sku;
        entity.Active = product.Active;
        entity.UpdatedAt = product.UpdatedAt;

        await _context.SaveChangesAsync();
        return product;
    }

    public async Task Delete(Guid id)
    {
        var entity = await _context.Products.FindAsync(id);
        if (entity is not null)
        {
            entity.Active = false;
            entity.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }
    }

    private static ProductEntity ToEntity(Product product)
    {
        return new ProductEntity
        {
            Id = product.Id,
            Name = product.Name,
            Description = product.Description,
            Price = product.Price,
            Category = product.Category,
            StockQuantity = product.StockQuantity,
            Sku = product.Sku,
            Active = product.Active,
            CreatedAt = product.CreatedAt,
            UpdatedAt = product.UpdatedAt
        };
    }

    private static Product ToDomain(ProductEntity entity)
    {
        // Using reflection-free approach: create via factory then set properties
        // In a real app, you might use a private constructor or builder
        return Product.Create(
            entity.Name,
            entity.Description,
            entity.Price,
            entity.Category,
            entity.StockQuantity,
            entity.Sku
        ).Reconstitute(entity.Id, entity.Active, entity.CreatedAt, entity.UpdatedAt);
    }
}
