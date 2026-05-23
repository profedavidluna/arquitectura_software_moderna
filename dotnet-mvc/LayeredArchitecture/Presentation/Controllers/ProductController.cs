using LayeredArchitecture.Business.Services;
using LayeredArchitecture.Data.Entities;
using LayeredArchitecture.Presentation.Dto;
using Microsoft.AspNetCore.Mvc;

namespace LayeredArchitecture.Presentation.Controllers;

/// <summary>
/// REST controller for Product operations.
/// This belongs to the PRESENTATION LAYER — the topmost layer in the layered architecture.
/// It depends directly on the Business layer (ProductService).
/// Dependencies flow downward: Presentation → Business → Data.
/// </summary>
[ApiController]
[Route("api/v1/products")]
[Produces("application/json")]
public class ProductController : ControllerBase
{
    private readonly ProductService _productService;

    public ProductController(ProductService productService)
    {
        _productService = productService;
    }

    /// <summary>
    /// Creates a new product.
    /// </summary>
    [HttpPost]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> CreateProduct([FromBody] CreateProductRequest request)
    {
        var entity = await _productService.CreateProduct(
            request.Name, request.Description ?? string.Empty, request.Price,
            request.Category, request.StockQuantity, request.Sku);

        var response = MapToResponse(entity);
        return CreatedAtAction(nameof(GetProductById), new { id = entity.Id }, response);
    }

    /// <summary>
    /// Lists products with pagination.
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(PaginatedResponse<ProductResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> ListProducts([FromQuery] int page = 0, [FromQuery] int pageSize = 20)
    {
        var (items, totalCount) = await _productService.ListProducts(page, pageSize);
        var effectivePageSize = Math.Min(Math.Max(pageSize, 1), 100);
        var totalPages = (int)Math.Ceiling((double)totalCount / effectivePageSize);

        var response = new PaginatedResponse<ProductResponse>(
            items.Select(MapToResponse),
            page,
            effectivePageSize,
            totalCount,
            totalPages
        );

        return Ok(response);
    }

    /// <summary>
    /// Gets a product by its ID.
    /// </summary>
    [HttpGet("{id:guid}")]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetProductById(Guid id)
    {
        var entity = await _productService.GetProductById(id);
        return Ok(MapToResponse(entity));
    }

    /// <summary>
    /// Searches products by name, category, or description.
    /// </summary>
    [HttpGet("search")]
    [ProducesResponseType(typeof(IEnumerable<ProductResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> SearchProducts([FromQuery] string query = "")
    {
        var entities = await _productService.SearchProducts(query);
        return Ok(entities.Select(MapToResponse));
    }

    /// <summary>
    /// Updates an existing product.
    /// </summary>
    [HttpPut("{id:guid}")]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateProduct(Guid id, [FromBody] UpdateProductRequest request)
    {
        var entity = await _productService.UpdateProduct(
            id, request.Name, request.Description ?? string.Empty,
            request.Price, request.Category, request.Sku);

        return Ok(MapToResponse(entity));
    }

    /// <summary>
    /// Soft-deletes a product (sets active=false).
    /// </summary>
    [HttpDelete("{id:guid}")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> DeleteProduct(Guid id)
    {
        await _productService.DeleteProduct(id);
        return NoContent();
    }

    /// <summary>
    /// Decreases product stock by the specified quantity.
    /// </summary>
    [HttpPatch("{id:guid}/stock/decrease")]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> DecreaseStock(Guid id, [FromQuery] int quantity)
    {
        var entity = await _productService.DecreaseStock(id, quantity);
        return Ok(MapToResponse(entity));
    }

    /// <summary>
    /// Increases product stock by the specified quantity.
    /// </summary>
    [HttpPatch("{id:guid}/stock/increase")]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> IncreaseStock(Guid id, [FromQuery] int quantity)
    {
        var entity = await _productService.IncreaseStock(id, quantity);
        return Ok(MapToResponse(entity));
    }

    private static ProductResponse MapToResponse(ProductEntity entity)
    {
        return new ProductResponse(
            entity.Id,
            entity.Name,
            entity.Description,
            entity.Price,
            entity.Category,
            entity.StockQuantity,
            entity.Sku,
            entity.Active,
            entity.CreatedAt,
            entity.UpdatedAt
        );
    }
}
