using HexagonalArchitecture.Adapters.Input.Web.Dto;
using HexagonalArchitecture.Domain.Models;
using HexagonalArchitecture.Domain.Ports.Input;
using Microsoft.AspNetCore.Mvc;

namespace HexagonalArchitecture.Adapters.Input.Web;

/// <summary>
/// REST controller acting as an INPUT ADAPTER in the Hexagonal Architecture.
/// It translates HTTP requests into calls to the input port (IProductService)
/// and maps domain objects back to DTOs for the response.
/// </summary>
[ApiController]
[Route("api/v1/products")]
[Produces("application/json")]
public class ProductController : ControllerBase
{
    private readonly IProductService _productService;

    public ProductController(IProductService productService)
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
        var product = await _productService.CreateProduct(
            request.Name, request.Description ?? string.Empty, request.Price,
            request.Category, request.StockQuantity, request.Sku);

        var response = MapToResponse(product);
        return CreatedAtAction(nameof(GetProductById), new { id = product.Id }, response);
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
        var product = await _productService.GetProductById(id);
        return Ok(MapToResponse(product));
    }

    /// <summary>
    /// Searches products by name or category.
    /// </summary>
    [HttpGet("search")]
    [ProducesResponseType(typeof(IEnumerable<ProductResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> SearchProducts([FromQuery] string query = "")
    {
        var products = await _productService.SearchProducts(query);
        return Ok(products.Select(MapToResponse));
    }

    /// <summary>
    /// Updates an existing product.
    /// </summary>
    [HttpPut("{id:guid}")]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateProduct(Guid id, [FromBody] UpdateProductRequest request)
    {
        var product = await _productService.UpdateProduct(
            id, request.Name, request.Description ?? string.Empty,
            request.Price, request.Category, request.Sku);

        return Ok(MapToResponse(product));
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
        var product = await _productService.DecreaseStock(id, quantity);
        return Ok(MapToResponse(product));
    }

    /// <summary>
    /// Increases product stock by the specified quantity.
    /// </summary>
    [HttpPatch("{id:guid}/stock/increase")]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> IncreaseStock(Guid id, [FromQuery] int quantity)
    {
        var product = await _productService.IncreaseStock(id, quantity);
        return Ok(MapToResponse(product));
    }

    private static ProductResponse MapToResponse(Product product)
    {
        return new ProductResponse(
            product.Id,
            product.Name,
            product.Description,
            product.Price,
            product.Category,
            product.StockQuantity,
            product.Sku,
            product.Active,
            product.CreatedAt,
            product.UpdatedAt
        );
    }
}
