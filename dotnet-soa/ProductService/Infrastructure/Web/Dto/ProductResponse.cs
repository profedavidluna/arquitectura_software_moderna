namespace ProductService.Infrastructure.Web.Dto;

/// <summary>
/// DTO for product API responses.
/// Decouples the API response format from the internal domain model.
/// </summary>
public record ProductResponse
{
    public Guid Id { get; init; }
    public string Name { get; init; } = string.Empty;
    public string? Description { get; init; }
    public decimal Price { get; init; }
    public string? Category { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime UpdatedAt { get; init; }
}
