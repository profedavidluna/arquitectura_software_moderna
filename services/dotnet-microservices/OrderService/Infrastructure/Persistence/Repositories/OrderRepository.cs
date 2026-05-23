namespace OrderService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using OrderService.Domain.Interfaces;
using OrderService.Domain.Models;
using OrderService.Infrastructure.Persistence.Entities;

public class OrderRepository : IOrderRepository
{
    private readonly AppDbContext _context;

    public OrderRepository(AppDbContext context) => _context = context;

    public async Task<Order?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Orders.Include(o => o.Items).FirstOrDefaultAsync(o => o.Id == id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<IEnumerable<Order>> GetByUserIdAsync(Guid userId)
    {
        var entities = await _context.Orders.Include(o => o.Items).Where(o => o.UserId == userId).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<IEnumerable<Order>> GetAllAsync()
    {
        var entities = await _context.Orders.Include(o => o.Items).ToListAsync();
        return entities.Select(MapToDomain);
    }

    public async Task<Order> CreateAsync(Order order)
    {
        var entity = new OrderEntity
        {
            Id = order.Id, UserId = order.UserId, Status = order.Status,
            TotalAmount = order.TotalAmount, ShippingAddress = order.ShippingAddress,
            SagaState = order.SagaState, CreatedAt = order.CreatedAt, UpdatedAt = order.UpdatedAt,
            Items = order.Items.Select(i => new OrderItemEntity
            {
                Id = i.Id == Guid.Empty ? Guid.NewGuid() : i.Id,
                OrderId = order.Id, ProductId = i.ProductId,
                ProductName = i.ProductName, UnitPrice = i.UnitPrice, Quantity = i.Quantity
            }).ToList()
        };
        _context.Orders.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Order> UpdateStatusAsync(Guid id, string status, string sagaState)
    {
        var entity = await _context.Orders.Include(o => o.Items).FirstOrDefaultAsync(o => o.Id == id)
            ?? throw new KeyNotFoundException($"Order {id} not found");
        entity.Status = status;
        entity.SagaState = sagaState;
        entity.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var entity = await _context.Orders.FindAsync(id);
        if (entity == null) return false;
        _context.Orders.Remove(entity);
        await _context.SaveChangesAsync();
        return true;
    }

    private static Order MapToDomain(OrderEntity e) => new(
        e.Id, e.UserId, e.Status, e.TotalAmount, e.ShippingAddress, e.SagaState,
        e.Items.Select(i => new OrderItem(i.Id, i.OrderId, i.ProductId, i.ProductName, i.UnitPrice, i.Quantity)).ToList(),
        e.CreatedAt, e.UpdatedAt
    );
}
