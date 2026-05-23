using Microsoft.EntityFrameworkCore;
using ProductService.Domain.Models;

namespace ProductService.Infrastructure.Persistence;

/// <summary>
/// Repository pattern implementation for Product persistence.
/// 
/// Design Pattern: Repository - abstracts data access logic and provides
/// a collection-like interface for domain objects.
/// </summary>
public class ProductRepository
{
    private readonly AppDbContext _context;

    public ProductRepository(AppDbContext context)
    {
        _context = context;
    }

    public virtual async Task<Product> CreateAsync(Product product)
    {
        var entity = new ProductEntity
        {
            Id = product.Id,
            Name = product.Name,
            Description = product.Description,
            Price = product.Price,
            Category = product.Category,
            CreatedAt = product.CreatedAt,
            UpdatedAt = product.UpdatedAt
        };

        _context.Products.Add(entity);
        await _context.SaveChangesAsync();

        return MapToDomain(entity);
    }

    public virtual async Task<IEnumerable<Product>> GetAllAsync()
    {
        var entities = await _context.Products.OrderByDescending(p => p.CreatedAt).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public virtual async Task<Product?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Products.FindAsync(id);
        return entity is null ? null : MapToDomain(entity);
    }

    public virtual async Task<bool> DeleteAsync(Guid id)
    {
        var entity = await _context.Products.FindAsync(id);
        if (entity is null) return false;

        _context.Products.Remove(entity);
        await _context.SaveChangesAsync();
        return true;
    }

    private static Product MapToDomain(ProductEntity entity) => new()
    {
        Id = entity.Id,
        Name = entity.Name,
        Description = entity.Description,
        Price = entity.Price,
        Category = entity.Category,
        CreatedAt = entity.CreatedAt,
        UpdatedAt = entity.UpdatedAt
    };
}
