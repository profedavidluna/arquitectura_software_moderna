using Microsoft.EntityFrameworkCore;
using InventoryService.Domain.Models;

namespace InventoryService.Infrastructure.Persistence;

/// <summary>
/// Repository pattern implementation for Inventory persistence.
/// Handles stock level management with reservation support.
/// </summary>
public class InventoryRepository
{
    private readonly AppDbContext _context;

    public InventoryRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<InventoryItem> CreateAsync(InventoryItem item)
    {
        var entity = new InventoryEntity
        {
            Id = item.Id,
            ProductId = item.ProductId,
            ProductName = item.ProductName,
            Quantity = item.Quantity,
            ReservedQuantity = item.ReservedQuantity,
            CreatedAt = item.CreatedAt,
            UpdatedAt = item.UpdatedAt
        };

        _context.Inventory.Add(entity);
        await _context.SaveChangesAsync();

        return MapToDomain(entity);
    }

    public async Task<IEnumerable<InventoryItem>> GetAllAsync()
    {
        var entities = await _context.Inventory.OrderBy(i => i.ProductName).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<InventoryItem?> GetByProductIdAsync(Guid productId)
    {
        var entity = await _context.Inventory.FirstOrDefaultAsync(i => i.ProductId == productId);
        return entity is null ? null : MapToDomain(entity);
    }

    public async Task<InventoryItem> UpdateQuantityAsync(Guid productId, int newQuantity)
    {
        var entity = await _context.Inventory.FirstAsync(i => i.ProductId == productId);
        entity.Quantity = newQuantity;
        entity.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task ReserveStockAsync(Guid productId, int quantity)
    {
        var entity = await _context.Inventory.FirstAsync(i => i.ProductId == productId);
        entity.ReservedQuantity += quantity;
        entity.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
    }

    public async Task ReleaseStockAsync(Guid productId, int quantity)
    {
        var entity = await _context.Inventory.FirstOrDefaultAsync(i => i.ProductId == productId);
        if (entity is not null)
        {
            entity.ReservedQuantity = Math.Max(0, entity.ReservedQuantity - quantity);
            entity.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }
    }

    private static InventoryItem MapToDomain(InventoryEntity entity) => new()
    {
        Id = entity.Id,
        ProductId = entity.ProductId,
        ProductName = entity.ProductName,
        Quantity = entity.Quantity,
        ReservedQuantity = entity.ReservedQuantity,
        CreatedAt = entity.CreatedAt,
        UpdatedAt = entity.UpdatedAt
    };
}
