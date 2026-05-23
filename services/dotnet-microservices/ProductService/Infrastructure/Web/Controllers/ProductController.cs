namespace ProductService.Infrastructure.Web.Controllers;

using Microsoft.AspNetCore.Mvc;
using ProductService.Domain.Interfaces;
using ProductService.Domain.Models;
using ProductService.Infrastructure.Web.Dto;

[ApiController]
[Route("api/[controller]")]
public class ProductController : ControllerBase
{
    private readonly IProductService _productService;

    public ProductController(IProductService productService) => _productService = productService;

    [HttpGet]
    public async Task<ActionResult<IEnumerable<ProductDto>>> GetAll()
    {
        var products = await _productService.GetAllProductsAsync();
        return Ok(products.Select(MapToDto));
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ProductDto>> GetById(Guid id)
    {
        var product = await _productService.GetProductByIdAsync(id);
        if (product == null) return NotFound();
        return Ok(MapToDto(product));
    }

    [HttpGet("search")]
    public async Task<ActionResult<IEnumerable<ProductDto>>> Search([FromQuery] string q)
    {
        var products = await _productService.SearchProductsAsync(q);
        return Ok(products.Select(MapToDto));
    }

    [HttpPost]
    public async Task<ActionResult<ProductDto>> Create([FromBody] CreateProductRequest request)
    {
        var product = await _productService.CreateProductAsync(request);
        return CreatedAtAction(nameof(GetById), new { id = product.Id }, MapToDto(product));
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<ProductDto>> Update(Guid id, [FromBody] UpdateProductRequest request)
    {
        try
        {
            var product = await _productService.UpdateProductAsync(id, request);
            return Ok(MapToDto(product));
        }
        catch (KeyNotFoundException) { return NotFound(); }
    }

    [HttpDelete("{id:guid}")]
    public async Task<ActionResult> Delete(Guid id)
    {
        var deleted = await _productService.DeleteProductAsync(id);
        if (!deleted) return NotFound();
        return NoContent();
    }

    [HttpPost("categories")]
    public async Task<ActionResult<CategoryDto>> CreateCategory([FromBody] CreateCategoryRequest request)
    {
        var category = await _productService.CreateCategoryAsync(request);
        return Created($"/api/product/categories/{category.Id}", new CategoryDto(category.Id, category.Name, category.Description, category.ParentId));
    }

    [HttpGet("categories")]
    public async Task<ActionResult<IEnumerable<CategoryDto>>> GetCategories()
    {
        var categories = await _productService.GetAllCategoriesAsync();
        return Ok(categories.Select(c => new CategoryDto(c.Id, c.Name, c.Description, c.ParentId)));
    }

    [HttpPost("{productId:guid}/reviews")]
    public async Task<ActionResult<ReviewDto>> AddReview(Guid productId, [FromBody] CreateReviewRequest request)
    {
        try
        {
            var review = await _productService.AddReviewAsync(productId, request);
            return Created($"/api/product/{productId}/reviews/{review.Id}", new ReviewDto(review.Id, review.ProductId, review.UserId, review.Rating, review.Comment, review.CreatedAt));
        }
        catch (KeyNotFoundException) { return NotFound(); }
    }

    [HttpGet("{productId:guid}/reviews")]
    public async Task<ActionResult<IEnumerable<ReviewDto>>> GetReviews(Guid productId)
    {
        var reviews = await _productService.GetProductReviewsAsync(productId);
        return Ok(reviews.Select(r => new ReviewDto(r.Id, r.ProductId, r.UserId, r.Rating, r.Comment, r.CreatedAt)));
    }

    private static ProductDto MapToDto(Product p) => new(p.Id, p.Name, p.Description, p.Price, p.CategoryId, p.ImageUrl, p.IsActive, p.CreatedAt);
}
