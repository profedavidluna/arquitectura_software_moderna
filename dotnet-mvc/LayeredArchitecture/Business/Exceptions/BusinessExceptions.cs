namespace LayeredArchitecture.Business.Exceptions;

/// <summary>
/// Exception thrown when a product is not found.
/// This belongs to the BUSINESS LAYER.
/// </summary>
public class ProductNotFoundException : Exception
{
    public Guid ProductId { get; }

    public ProductNotFoundException(Guid id)
        : base($"Product with ID '{id}' was not found.")
    {
        ProductId = id;
    }
}

/// <summary>
/// Exception thrown when a business rule is violated.
/// This belongs to the BUSINESS LAYER.
/// </summary>
public class BusinessRuleException : Exception
{
    public BusinessRuleException(string message) : base(message) { }
}

/// <summary>
/// Exception thrown when a duplicate SKU is detected.
/// This belongs to the BUSINESS LAYER.
/// </summary>
public class DuplicateSkuException : BusinessRuleException
{
    public DuplicateSkuException(string sku)
        : base($"A product with SKU '{sku}' already exists.") { }
}

/// <summary>
/// Exception thrown when stock is insufficient for an operation.
/// This belongs to the BUSINESS LAYER.
/// </summary>
public class InsufficientStockException : BusinessRuleException
{
    public InsufficientStockException()
        : base("Insufficient stock. Cannot decrease below zero.") { }
}
