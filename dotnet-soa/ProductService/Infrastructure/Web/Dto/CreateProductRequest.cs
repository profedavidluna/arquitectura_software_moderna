using System.ComponentModel.DataAnnotations;

namespace ProductService.Infrastructure.Web.Dto;

/// <summary>
/// DTO for product creation requests.
/// Design Pattern: DTO (Data Transfer Object) - separates API contract from domain model.
/// </summary>
public record CreateProductRequest
{
    [Required]
    [StringLength(255)]
    public string Name { get; init; } = string.Empty;

    public string? Description { get; init; }

    [Required]
    [Range(0.01, double.MaxValue, ErrorMessage = "Price must be greater than zero")]
    public decimal Price { get; init; }

    [StringLength(100)]
    public string? Category { get; init; }
}
