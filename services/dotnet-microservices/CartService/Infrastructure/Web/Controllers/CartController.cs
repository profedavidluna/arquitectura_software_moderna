namespace CartService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using CartService.Domain.Interfaces;
using CartService.Domain.Models;
using CartService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class CartController : ControllerBase
{
    private readonly ICartService _cartService;

    public CartController(ICartService cartService) => _cartService = cartService;

    [HttpGet("{userId:guid}")]
    public async Task<ActionResult<CartDto>> GetCart(Guid userId)
    {
        var cart = await _cartService.GetOrCreateCartAsync(userId);
        return Ok(MapToDto(cart));
    }

    [HttpPost("{userId:guid}/items")]
    public async Task<ActionResult<CartDto>> AddItem(Guid userId, [FromBody] AddItemRequest request)
    {
        var cart = await _cartService.AddItemToCartAsync(userId, request);
        return Ok(MapToDto(cart));
    }

    [HttpPut("{userId:guid}/items/{itemId:guid}")]
    public async Task<ActionResult<CartDto>> UpdateItemQuantity(Guid userId, Guid itemId, [FromBody] UpdateItemQuantityRequest request)
    {
        try
        {
            var cart = await _cartService.UpdateItemQuantityAsync(userId, itemId, request);
            return Ok(MapToDto(cart));
        }
        catch (KeyNotFoundException) { return NotFound(); }
    }

    [HttpDelete("{userId:guid}/items/{itemId:guid}")]
    public async Task<ActionResult<CartDto>> RemoveItem(Guid userId, Guid itemId)
    {
        try
        {
            var cart = await _cartService.RemoveItemFromCartAsync(userId, itemId);
            return Ok(MapToDto(cart));
        }
        catch (KeyNotFoundException) { return NotFound(); }
    }

    [HttpPost("{userId:guid}/coupon")]
    public async Task<ActionResult<CartDto>> ApplyCoupon(Guid userId, [FromBody] ApplyCouponRequest request)
    {
        try
        {
            var cart = await _cartService.ApplyCouponAsync(userId, request);
            return Ok(MapToDto(cart));
        }
        catch (KeyNotFoundException) { return NotFound(); }
        catch (InvalidOperationException ex) { return BadRequest(new { message = ex.Message }); }
    }

    [HttpDelete("{userId:guid}")]
    public async Task<ActionResult> ClearCart(Guid userId)
    {
        var cleared = await _cartService.ClearCartAsync(userId);
        if (!cleared) return NotFound();
        return NoContent();
    }

    private static CartDto MapToDto(Cart c) => new(
        c.Id, c.UserId, c.Status, c.CouponCode, c.DiscountPercent,
        c.Items.Select(i => new CartItemDto(i.Id, i.ProductId, i.ProductName, i.UnitPrice, i.Quantity)).ToList(),
        c.Subtotal, c.DiscountAmount, c.Total
    );
}
