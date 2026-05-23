namespace CartService.Domain.Interfaces;

using CartService.Domain.Models;

public interface ICartRepository
{
    Task<Cart?> GetByIdAsync(Guid id);
    Task<Cart?> GetByUserIdAsync(Guid userId);
    Task<Cart> CreateAsync(Cart cart);
    Task<Cart> UpdateAsync(Cart cart);
    Task<CartItem> AddItemAsync(CartItem item);
    Task<bool> RemoveItemAsync(Guid itemId);
    Task<CartItem?> GetItemAsync(Guid itemId);
    Task<CartItem> UpdateItemAsync(CartItem item);
    Task<bool> ClearCartAsync(Guid cartId);
}

public interface ICartService
{
    Task<Cart> GetOrCreateCartAsync(Guid userId);
    Task<Cart> AddItemToCartAsync(Guid userId, AddItemRequest request);
    Task<Cart> UpdateItemQuantityAsync(Guid userId, Guid itemId, UpdateItemQuantityRequest request);
    Task<Cart> RemoveItemFromCartAsync(Guid userId, Guid itemId);
    Task<Cart> ApplyCouponAsync(Guid userId, ApplyCouponRequest request);
    Task<bool> ClearCartAsync(Guid userId);
}
