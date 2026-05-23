namespace InventoryService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using InventoryService.Domain.Interfaces;
using InventoryService.Domain.Models;
using InventoryService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class InventoryController : ControllerBase
{
    private readonly IInventoryService _inventoryService;

    public InventoryController(IInventoryService inventoryService) => _inventoryService = inventoryService;

    [HttpGet]
    public async Task<ActionResult<IEnumerable<InventoryItemDto>>> GetAll()
    {
        var items = await _inventoryService.GetAllAsync();
        return Ok(items.Select(MapToDto));
    }

    [HttpGet("product/{productId:guid}")]
    public async Task<ActionResult<InventoryItemDto>> GetByProductId(Guid productId)
    {
        var item = await _inventoryService.GetByProductIdAsync(productId);
        if (item == null) return NotFound();
        return Ok(MapToDto(item));
    }

    [HttpPost]
    public async Task<ActionResult<InventoryItemDto>> Create([FromBody] CreateInventoryItemRequest request)
    {
        try
        {
            var item = await _inventoryService.CreateItemAsync(request);
            return Created($"/api/inventory/product/{item.ProductId}", MapToDto(item));
        }
        catch (InvalidOperationException ex) { return Conflict(new { message = ex.Message }); }
    }

    [HttpPatch("product/{productId:guid}/stock")]
    public async Task<ActionResult<InventoryItemDto>> UpdateStock(Guid productId, [FromBody] UpdateStockRequest request)
    {
        try
        {
            var item = await _inventoryService.UpdateStockAsync(productId, request);
            return Ok(MapToDto(item));
        }
        catch (KeyNotFoundException) { return NotFound(); }
        catch (InvalidOperationException ex) { return BadRequest(new { message = ex.Message }); }
    }

    [HttpPost("reserve")]
    public async Task<ActionResult> ReserveStock([FromBody] ReserveStockRequest request)
    {
        var success = await _inventoryService.ReserveStockAsync(request);
        if (!success) return BadRequest(new { message = "Insufficient stock" });
        return Ok(new { message = "Stock reserved" });
    }

    [HttpPost("release/{orderId:guid}")]
    public async Task<ActionResult> ReleaseReservation(Guid orderId)
    {
        await _inventoryService.ReleaseReservationAsync(orderId);
        return Ok(new { message = "Reservation released" });
    }

    private static InventoryItemDto MapToDto(InventoryItem i) => new(
        i.Id, i.ProductId, i.ProductName, i.Quantity, i.ReservedQuantity,
        i.AvailableQuantity, i.MinStockLevel, i.IsLowStock
    );
}
