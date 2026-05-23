using LayeredArchitecture.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace LayeredArchitecture.Data.Repositories;

/// <summary>
/// Repository for Product data access operations.
/// This belongs to the DATA LAYER. In a layered architecture, the repository
/// directly exposes entities to the business layer (tighter coupling).
/// </summary>
public class ProductRepository
{
    private readonly AppDbContext _context;

    public ProductRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<ProductEntity> Save(ProductEntity entity)
    {
        _context.Products.Add(entity);
        await _context.SaveChangesAsync();
        return entity;
    }

    public async Task<ProductEntity?> FindById(Guid id)
    {
        return await _context.Products.FindAsync(id);
    }

    public async Task<(List<ProductEntity> Items, int TotalCount)> FindAll(int page, int pageSize)
    {
        var totalCount = await _context.Products.Where(p => p.Active).CountAsync();
        var items = await _context.Products
            .Where(p => p.Active)
            .OrderByDescending(p => p.CreatedAt)
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<List<ProductEntity>> Search(string query)
    {
        var lowerQuery = query.ToLower();
        return await _context.Products
            .Where(p => p.Active &&
                (p.Name.ToLower().Contains(lowerQuery) ||
                 p.Category.ToLower().Contains(lowerQuery) ||
                 p.Description.ToLower().Contains(lowerQuery)))
            .OrderByDescending(p => p.CreatedAt)
            .Take(100)
            .ToListAsync();
    }

    public async Task<bool> ExistsBySku(string sku)
    {
        return await _context.Products.AnyAsync(p => p.Sku == sku);
    }

    public async Task<bool> ExistsBySkuAndIdNot(string sku, Guid id)
    {
        return await _context.Products.AnyAsync(p => p.Sku == sku && p.Id != id);
    }

    public async Task<ProductEntity> Update(ProductEntity entity)
    {
        _context.Products.Update(entity);
        await _context.SaveChangesAsync();
        return entity;
    }
}
