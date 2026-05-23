namespace CartService.Infrastructure.Persistence.Repositories;

using Microsoft.EntityFrameworkCore;
using CartService.Domain.Interfaces;
using CartService.Domain.Models;
using CartService.Infrastructure.Persistence.Entities;

public class CartRepository : ICartRepository
{
    private readonly AppDbContext _context;

    public CartRepository(AppDbContext context) => _context = context;

    public async Task<Cart?> GetByIdAsync(Guid id)
    {
        var entity = await _context.Carts.Include(c => c.Items).FirstOrDefaultAsync(c => c.Id == id);
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<Cart?> GetByUserIdAsync(Guid userId)
    {
        var entity = await _context.Carts.Include(c => c.Items)
            .FirstOrDefaultAsync(c => c.UserId == userId && c.Status == "active");
        return entity == null ? null : MapToDomain(entity);
    }

    public async Task<Cart> CreateAsync(Cart cart)
    {
        var entity = new CartEntity
        {
            Id = cart.Id, UserId = cart.UserId, Status = cart.Status,
            CouponCode = cart.CouponCode, DiscountPercent = cart.DiscountPercent,
            CreatedAt = cart.CreatedAt, UpdatedAt = cart.UpdatedAt
        };
        _context.Carts.Add(entity);
        await _context.SaveChangesAsync();
        return MapToDomain(entity);
    }

    public async Task<Cart> UpdateAsync(Cart cart)
    {
        var entity = await _context.Carts.FindAsync(cart.Id)
            ?? throw new KeyNotFoundException("Cart not found");
        entity.Status = cart.Status;
        entity.CouponCode = cart.CouponCode;
        entity.DiscountPercent = cart.DiscountPercent;
        entity.UpdatedAt = cart.UpdatedAt;
        await _context.SaveChangesAsync();
        return (await GetByIdAsync(cart.Id))!;
    }

    public async Task<CartItem> AddItemAsync(CartItem item)
    {
        var entity = new CartItemEntity
        {
            Id = item.Id, CartId = item.CartId, ProductId = item.ProductId,
            ProductName = item.ProductName, UnitPrice = item.UnitPrice,
            Quantity = item.Quantity, CreatedAt = item.CreatedAt
        };
        _context.CartItems.Add(entity);
        await _context.SaveChangesAsync();
        return MapItemToDomain(entity);
    }

    public async Task<bool> RemoveItemAsync(Guid itemId)
    {
        var entity = await _context.CartItems.FindAsync(itemId);
        if (entity == null) return false;
        _context.CartItems.Remove(entity);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<CartItem?> GetItemAsync(Guid itemId)
    {
        var entity = await _context.CartItems.FindAsync(itemId);
        return entity == null ? null : MapItemToDomain(entity);
    }

    public async Task<CartItem> UpdateItemAsync(CartItem item)
    {
        var entity = await _context.CartItems.FindAsync(item.Id)
            ?? throw new KeyNotFoundException("Item not found");
        entity.Quantity = item.Quantity;
        await _context.SaveChangesAsync();
        return MapItemToDomain(entity);
    }

    public async Task<bool> ClearCartAsync(Guid cartId)
    {
        var items = await _context.CartItems.Where(i => i.CartId == cartId).ToListAsync();
        _context.CartItems.RemoveRange(items);
        await _context.SaveChangesAsync();
        return true;
    }

    private static Cart MapToDomain(CartEntity e) => new(
        e.Id, e.UserId, e.Status, e.CouponCode, e.DiscountPercent,
        e.Items.Select(MapItemToDomain).ToList(), e.CreatedAt, e.UpdatedAt
    );

    private static CartItem MapItemToDomain(CartItemEntity e) => new(
        e.Id, e.CartId, e.ProductId, e.ProductName, e.UnitPrice, e.Quantity, e.CreatedAt
    );
}
