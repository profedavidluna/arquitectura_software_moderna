using System.ComponentModel.DataAnnotations;

namespace HexagonalArchitecture.Adapters.Input.Web.Dto;

/// <summary>
/// DTO for creating a new product. Belongs to the INPUT ADAPTER layer.
/// Uses DataAnnotations for basic request validation.
/// </summary>
public record CreateProductRequest(
    [Required] string Name,
    string? Description,
    [Required][Range(0.01, double.MaxValue, ErrorMessage = "Price must be greater than zero.")] decimal Price,
    [Required] string Category,
    [Required][Range(0, int.MaxValue)] int StockQuantity,
    [Required] string Sku
);

/// <summary>
/// DTO for updating an existing product. Belongs to the INPUT ADAPTER layer.
/// </summary>
public record UpdateProductRequest(
    [Required] string Name,
    string? Description,
    [Required][Range(0.01, double.MaxValue, ErrorMessage = "Price must be greater than zero.")] decimal Price,
    [Required] string Category,
    [Required] string Sku
);
