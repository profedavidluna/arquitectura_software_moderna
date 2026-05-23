using Microsoft.AspNetCore.Mvc;
using InventoryService.Domain.Interfaces;
using InventoryService.Infrastructure.Web.Dto;

namespace InventoryService.Infrastructure.Web;

/// <summary>
/// REST API controller for Inventory Service.
/// 
/// SOA Principle: Service Discoverability - exposes inventory management
/// through a standardized REST interface.
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class InventoryController : ControllerBase
{
    private readonly IInventoryService _inventoryService;
    private readonly ILogger<InventoryController> _logger;

    public InventoryController(IInventoryService inventoryService, ILogger<InventoryController> logger)
    {
        _inventoryService = inventoryService;
        _logger = logger;
    }

    /// <summary>GET /api/inventory - Retrieve all inventory items</summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<InventoryResponse>>> GetAll()
    {
        var items = await _inventoryService.GetAllInventoryAsync();
        var response = items.Select(MapToResponse);
        return Ok(response);
    }

    /// <summary>GET /api/inventory/{productId} - Retrieve inventory by product ID</summary>
    [HttpGet("{productId:guid}")]
    public async Task<ActionResult<InventoryResponse>> GetByProductId(Guid productId)
    {
        var item = await _inventoryService.GetByProductIdAsync(productId);
        if (item is null)
            return NotFound(new { message = $"Inventory for product {productId} not found" });

        return Ok(MapToResponse(item));
    }

    /// <summary>POST /api/inventory - Create or update inventory for a product</summary>
    [HttpPost]
    public async Task<ActionResult<InventoryResponse>> CreateOrUpdate([FromBody] CreateInventoryRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        _logger.LogInformation("Creating/updating inventory for product {ProductId}: {Quantity} units",
            request.ProductId, request.Quantity);

        var item = await _inventoryService.CreateOrUpdateInventoryAsync(
            request.ProductId, request.ProductName, request.Quantity);

        return Ok(MapToResponse(item));
    }

    private static InventoryResponse MapToResponse(Domain.Models.InventoryItem item) => new()
    {
        Id = item.Id,
        ProductId = item.ProductId,
        ProductName = item.ProductName,
        Quantity = item.Quantity,
        ReservedQuantity = item.ReservedQuantity,
        AvailableQuantity = item.AvailableQuantity,
        CreatedAt = item.CreatedAt,
        UpdatedAt = item.UpdatedAt
    };
}
