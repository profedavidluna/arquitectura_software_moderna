namespace ProductService.Domain.Models;

/// <summary>
/// Product domain model representing the core business entity.
/// 
/// SOA Principle: Service contracts are defined through domain models
/// that represent the service's bounded context.
/// </summary>
public record Product
{
    public Guid Id { get; init; } = Guid.NewGuid();
    public required string Name { get; init; }
    public string? Description { get; init; }
    public required decimal Price { get; init; }
    public string? Category { get; init; }
    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; init; } = DateTime.UtcNow;
}
