namespace InventoryService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using InventoryService.Domain.Interfaces;
using InventoryService.Domain.Models;
using InventoryService.Infrastructure.Persistence.Entities;

public class InventoryRepository : IInventoryRepository
{
    private readonly AppDbContext _context;

    public InventoryRepository(AppDbContext context) => _context = context;

    public async Task<InventoryItem?> GetByIdAsync(Guid id)
    {
        var entity = await _context.InventoryItems.FindAsync(id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<InventoryItem?> GetByProductIdAsync(Guid productId)
    {
        var entity = await _context.InventoryItems.FirstOrDefaultAsync(i => i.ProductId == productId);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<IEnumerable<InventoryItem>> GetAllAsync()
    {
        var entities = await _context.InventoryItems.ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<InventoryItem> CreateAsync(InventoryItem item)
    {
        var entity = new InventoryItemEntity
        {
            Id = item.Id, ProductId = item.ProductId, ProductName = item.ProductName,
            Quantity = item.Quantity, ReservedQuantity = item.ReservedQuantity,
            MinStockLevel = item.MinStockLevel, CreatedAt = item.CreatedAt, UpdatedAt = item.UpdatedAt
        };
        _context.InventoryItems.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<InventoryItem> UpdateAsync(InventoryItem item)
    {
        var entity = await _context.InventoryItems.FirstOrDefaultAsync(i => i.ProductId == item.ProductId)
            ?? throw new KeyNotFoundException($"Inventory item not found");
        entity.Quantity = item.Quantity;
        entity.ReservedQuantity = item.ReservedQuantity;
        entity.MinStockLevel = item.MinStockLevel;
        entity.UpdatedAt = item.UpdatedAt;
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Reservation> CreateReservationAsync(Reservation reservation)
    {
        var entity = new ReservationEntity
        {
            Id = reservation.Id, OrderId = reservation.OrderId, ProductId = reservation.ProductId,
            Quantity = reservation.Quantity, Status = reservation.Status,
            ExpiresAt = reservation.ExpiresAt, CreatedAt = reservation.CreatedAt
        };
        _context.Reservations.Add(entity);
        await _context.SaveChangesAsync();
        return MapReservationToDomain(entity);
    }

    public async Task<IEnumerable<Reservation>> GetReservationsByOrderIdAsync(Guid orderId)
    {
        var entities = await _context.Reservations.Where(r => r.OrderId == orderId).ToListAsync();
        return entities.Select(MapReservationToDomain);
    }

    public async Task<bool> UpdateReservationStatusAsync(Guid orderId, string status)
    {
        var entities = await _context.Reservations.Where(r => r.OrderId == orderId).ToListAsync();
        foreach (var entity in entities) entity.Status = status;
        await _context.SaveChangesAsync();
        return true;
    }

    private static InventoryItem MapToDomain(InventoryItemEntity e) => new(
        e.Id, e.ProductId, e.ProductName, e.Quantity, e.ReservedQuantity, e.MinStockLevel, e.CreatedAt, e.UpdatedAt
    );

    private static Reservation MapReservationToDomain(ReservationEntity e) => new(
        e.Id, e.OrderId, e.ProductId, e.Quantity, e.Status, e.ExpiresAt, e.CreatedAt
    );
}
