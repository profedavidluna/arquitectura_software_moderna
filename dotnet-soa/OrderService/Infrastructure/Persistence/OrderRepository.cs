using Microsoft.EntityFrameworkCore;
using OrderService.Domain.Models;

namespace OrderService.Infrastructure.Persistence;

/// <summary>
/// Repository pattern implementation for Order persistence.
/// Handles mapping between domain models and persistence entities.
/// </summary>
public class OrderRepository
{
    private readonly AppDbContext _context;

    public OrderRepository(AppDbContext context)
    {
        _context = context;
    }

    public virtual async Task<Order> CreateAsync(Order order)
    {
        var entity = new OrderEntity
        {
            Id = order.Id,
            CustomerName = order.CustomerName,
            CustomerEmail = order.CustomerEmail,
            Status = order.Status.ToString(),
            TotalAmount = order.TotalAmount,
            CreatedAt = order.CreatedAt,
            UpdatedAt = order.UpdatedAt,
            Items = order.Items.Select(i => new OrderItemEntity
            {
                Id = i.Id,
                OrderId = order.Id,
                ProductId = i.ProductId,
                ProductName = i.ProductName,
                Quantity = i.Quantity,
                UnitPrice = i.UnitPrice,
                CreatedAt = i.CreatedAt
            }).ToList()
        };

        _context.Orders.Add(entity);
        await _context.SaveChangesAsync();

        return MapToDomain(entity);
    }

    public virtual async Task<IEnumerable<Order>> GetAllAsync()
    {
        var entities = await _context.Orders
            .Include(o => o.Items)
            .OrderByDescending(o => o.CreatedAt)
            .ToListAsync();

        return entities.Select(MapToDomain);
    }

    public virtual async Task<Order?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Orders
            .Include(o => o.Items)
            .FirstOrDefaultAsync(o => o.Id == id);

        return entity is null ? null : MapToDomain(entity);
    }

    public virtual async Task UpdateStatusAsync(Guid id, OrderStatus status)
    {
        var entity = await _context.Orders.FindAsync(id);
        if (entity is not null)
        {
            entity.Status = status.ToString();
            entity.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }
    }

    private static Order MapToDomain(OrderEntity entity) => new()
    {
        Id = entity.Id,
        CustomerName = entity.CustomerName,
        CustomerEmail = entity.CustomerEmail,
        Status = Enum.Parse<OrderStatus>(entity.Status),
        TotalAmount = entity.TotalAmount,
        Items = entity.Items.Select(i => new OrderItem
        {
            Id = i.Id,
            OrderId = i.OrderId,
            ProductId = i.ProductId,
            ProductName = i.ProductName,
            Quantity = i.Quantity,
            UnitPrice = i.UnitPrice,
            CreatedAt = i.CreatedAt
        }).ToList(),
        CreatedAt = entity.CreatedAt,
        UpdatedAt = entity.UpdatedAt
    };
}
