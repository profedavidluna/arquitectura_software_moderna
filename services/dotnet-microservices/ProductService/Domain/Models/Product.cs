namespace ProductService.Domain.Models;

public record Product(
    Guid Id,
    string Name,
    string? Description,
    decimal Price,
    Guid? CategoryId,
    string? ImageUrl,
    bool IsActive,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

public record Category(
    Guid Id,
    string Name,
    string? Description,
    Guid? ParentId,
    DateTime CreatedAt
);

public record Review(
    Guid Id,
    Guid ProductId,
    Guid UserId,
    int Rating,
    string? Comment,
    DateTime CreatedAt
);

public record CreateProductRequest(
    string Name,
    string? Description,
    decimal Price,
    Guid? CategoryId,
    string? ImageUrl
);

public record UpdateProductRequest(
    string? Name,
    string? Description,
    decimal? Price,
    Guid? CategoryId,
    string? ImageUrl,
    bool? IsActive
);

public record CreateCategoryRequest(string Name, string? Description, Guid? ParentId);

public record CreateReviewRequest(Guid UserId, int Rating, string? Comment);
