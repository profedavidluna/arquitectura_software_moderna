using Microsoft.AspNetCore.Mvc;
using ProductService.Domain.Interfaces;
using ProductService.Infrastructure.Web.Dto;

namespace ProductService.Infrastructure.Web;

/// <summary>
/// REST API controller for Product Service.
/// 
/// SOA Principle: Service Discoverability - exposes well-defined
/// REST endpoints that other services and clients can discover and consume.
/// 
/// Design Pattern: Controller - handles HTTP concerns and delegates
/// business logic to the service layer (SRP).
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class ProductController : ControllerBase
{
    private readonly IProductService _productService;
    private readonly ILogger<ProductController> _logger;

    public ProductController(IProductService productService, ILogger<ProductController> logger)
    {
        _productService = productService;
        _logger = logger;
    }

    /// <summary>GET /api/product - Retrieve all products</summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<ProductResponse>>> GetAll()
    {
        var products = await _productService.GetAllProductsAsync();
        var response = products.Select(p => new ProductResponse
        {
            Id = p.Id,
            Name = p.Name,
            Description = p.Description,
            Price = p.Price,
            Category = p.Category,
            CreatedAt = p.CreatedAt,
            UpdatedAt = p.UpdatedAt
        });
        return Ok(response);
    }

    /// <summary>GET /api/product/{id} - Retrieve a product by ID</summary>
    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ProductResponse>> GetById(Guid id)
    {
        var product = await _productService.GetProductByIdAsync(id);
        if (product is null)
            return NotFound(new { message = $"Product with id {id} not found" });

        return Ok(new ProductResponse
        {
            Id = product.Id,
            Name = product.Name,
            Description = product.Description,
            Price = product.Price,
            Category = product.Category,
            CreatedAt = product.CreatedAt,
            UpdatedAt = product.UpdatedAt
        });
    }

    /// <summary>POST /api/product - Create a new product</summary>
    [HttpPost]
    public async Task<ActionResult<ProductResponse>> Create([FromBody] CreateProductRequest request)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        _logger.LogInformation("Creating product: {Name}", request.Name);

        var product = await _productService.CreateProductAsync(
            request.Name, request.Description, request.Price, request.Category);

        var response = new ProductResponse
        {
            Id = product.Id,
            Name = product.Name,
            Description = product.Description,
            Price = product.Price,
            Category = product.Category,
            CreatedAt = product.CreatedAt,
            UpdatedAt = product.UpdatedAt
        };

        return CreatedAtAction(nameof(GetById), new { id = product.Id }, response);
    }

    /// <summary>DELETE /api/product/{id} - Delete a product</summary>
    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        var deleted = await _productService.DeleteProductAsync(id);
        if (!deleted)
            return NotFound(new { message = $"Product with id {id} not found" });

        return NoContent();
    }
}
