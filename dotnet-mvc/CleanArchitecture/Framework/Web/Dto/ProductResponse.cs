namespace CleanArchitecture.Framework.Web.Dto;

/// <summary>
/// DTO for product responses. Belongs to the FRAMEWORK layer (outermost).
/// Maps from the domain entity to the API response format.
/// </summary>
public record ProductResponse(
    Guid Id,
    string Name,
    string Description,
    decimal Price,
    string Category,
    int StockQuantity,
    string Sku,
    bool Active,
    DateTime CreatedAt,
    DateTime UpdatedAt
);

/// <summary>
/// Paginated response wrapper.
/// </summary>
public record PaginatedResponse<T>(
    IEnumerable<T> Content,
    int Page,
    int PageSize,
    int TotalElements,
    int TotalPages
);
