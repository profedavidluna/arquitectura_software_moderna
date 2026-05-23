using CleanArchitecture.Entities;
using CleanArchitecture.Framework.Web.Dto;
using CleanArchitecture.UseCases;
using Microsoft.AspNetCore.Mvc;

namespace CleanArchitecture.Framework.Web;

/// <summary>
/// REST controller for Product operations.
/// This belongs to the FRAMEWORK layer (outermost) — it is a delivery mechanism detail.
/// It delegates to individual use cases, following the Single Responsibility Principle.
/// The controller knows about use cases but use cases don't know about the controller.
/// </summary>
[ApiController]
[Route("api/v1/products")]
[Produces("application/json")]
public class ProductController : ControllerBase
{
    private readonly CreateProductUseCase _createProduct;
    private readonly GetProductUseCase _getProduct;
    private readonly ListProductsUseCase _listProducts;
    private readonly SearchProductsUseCase _searchProducts;
    private readonly UpdateProductUseCase _updateProduct;
    private readonly DeleteProductUseCase _deleteProduct;
    private readonly ManageStockUseCase _manageStock;

    public ProductController(
        CreateProductUseCase createProduct,
        GetProductUseCase getProduct,
        ListProductsUseCase listProducts,
        SearchProductsUseCase searchProducts,
        UpdateProductUseCase updateProduct,
        DeleteProductUseCase deleteProduct,
        ManageStockUseCase manageStock)
    {
        _createProduct = createProduct;
        _getProduct = getProduct;
        _listProducts = listProducts;
        _searchProducts = searchProducts;
        _updateProduct = updateProduct;
        _deleteProduct = deleteProduct;
        _manageStock = manageStock;
    }

    /// <summary>
    /// Creates a new product.
    /// </summary>
    [HttpPost]
    [ProducesResponseType(typeof(ProductResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ProblemDetails), StatusCodes.Status400BadRequest)]
    public async Task<IActionResult> CreateProduct([FromBody] CreateProductRequest request)
    {
        var product = await _createProduct.Execute(
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
        var (items, totalCount) = await _listProducts.Execute(page, pageSize);
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
        var product = await _getProduct.Execute(id);
        return Ok(MapToResponse(product));
    }

    /// <summary>
    /// Searches products by name, category, or description.
    /// </summary>
    [HttpGet("search")]
    [ProducesResponseType(typeof(IEnumerable<ProductResponse>), StatusCodes.Status200OK)]
    public async Task<IActionResult> SearchProducts([FromQuery] string query = "")
    {
        var products = await _searchProducts.Execute(query);
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
        var product = await _updateProduct.Execute(
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
        await _deleteProduct.Execute(id);
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
        var product = await _manageStock.DecreaseStock(id, quantity);
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
        var product = await _manageStock.IncreaseStock(id, quantity);
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
