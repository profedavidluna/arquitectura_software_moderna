using System.ComponentModel.DataAnnotations;

namespace CleanArchitecture.Framework.Web.Dto;

/// <summary>
/// DTO for creating a new product. Belongs to the FRAMEWORK layer (outermost).
/// Framework-specific concern: request validation via DataAnnotations.
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
/// DTO for updating an existing product. Belongs to the FRAMEWORK layer (outermost).
/// </summary>
public record UpdateProductRequest(
    [Required] string Name,
    string? Description,
    [Required][Range(0.01, double.MaxValue, ErrorMessage = "Price must be greater than zero.")] decimal Price,
    [Required] string Category,
    [Required] string Sku
);
