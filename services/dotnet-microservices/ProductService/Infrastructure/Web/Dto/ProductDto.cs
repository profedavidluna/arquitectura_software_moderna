namespace ProductService.Infrastructure.Web.Dto;

public record ProductDto(Guid Id, string Name, string? Description, decimal Price, Guid? CategoryId, string? ImageUrl, bool IsActive, DateTime CreatedAt);
public record CategoryDto(Guid Id, string Name, string? Description, Guid? ParentId);
public record ReviewDto(Guid Id, Guid ProductId, Guid UserId, int Rating, string? Comment, DateTime CreatedAt);
