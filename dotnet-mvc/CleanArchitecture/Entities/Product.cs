namespace CleanArchitecture.Entities;

/// <summary>
/// Enterprise Business Entity — the innermost layer of Clean Architecture.
/// Contains enterprise-wide business rules that would exist even if the system were manual.
/// This entity has NO dependencies on any outer layer (use cases, adapters, or frameworks).
/// </summary>
public class Product
{
    public Guid Id { get; private set; }
    public string Name { get; private set; } = string.Empty;
    public string Description { get; private set; } = string.Empty;
    public decimal Price { get; private set; }
    public string Category { get; private set; } = string.Empty;
    public int StockQuantity { get; private set; }
    public string Sku { get; private set; } = string.Empty;
    public bool Active { get; private set; }
    public DateTime CreatedAt { get; private set; }
    public DateTime UpdatedAt { get; private set; }

    private Product() { }

    /// <summary>
    /// Factory method enforcing enterprise business rules for product creation.
    /// </summary>
    public static Product Create(string name, string description, decimal price, string category, int stockQuantity, string sku)
    {
        ValidateName(name);
        ValidatePrice(price);
        ValidateSku(sku);
        ValidateStock(stockQuantity);

        return new Product
        {
            Id = Guid.NewGuid(),
            Name = name,
            Description = description ?? string.Empty,
            Price = price,
            Category = category ?? string.Empty,
            StockQuantity = stockQuantity,
            Sku = sku,
            Active = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
    }

    /// <summary>
    /// Reconstitutes a product from persistence (used by gateways).
    /// </summary>
    public static Product Reconstitute(Guid id, string name, string description, decimal price,
        string category, int stockQuantity, string sku, bool active, DateTime createdAt, DateTime updatedAt)
    {
        return new Product
        {
            Id = id,
            Name = name,
            Description = description,
            Price = price,
            Category = category,
            StockQuantity = stockQuantity,
            Sku = sku,
            Active = active,
            CreatedAt = createdAt,
            UpdatedAt = updatedAt
        };
    }

    /// <summary>
    /// Enterprise rule: update product attributes.
    /// </summary>
    public void Update(string name, string description, decimal price, string category, string sku)
    {
        ValidateName(name);
        ValidatePrice(price);

        Name = name;
        Description = description ?? string.Empty;
        Price = price;
        Category = category ?? string.Empty;
        Sku = sku;
        UpdatedAt = DateTime.UtcNow;
    }

    /// <summary>
    /// Enterprise rule: stock cannot go below zero.
    /// </summary>
    public void DecreaseStock(int quantity)
    {
        if (quantity <= 0)
            throw new EntityValidationException("Quantity must be greater than zero.");

        if (StockQuantity - quantity < 0)
            throw new EntityValidationException("Insufficient stock. Cannot decrease below zero.");

        StockQuantity -= quantity;
        UpdatedAt = DateTime.UtcNow;
    }

    /// <summary>
    /// Enterprise rule: increase stock.
    /// </summary>
    public void IncreaseStock(int quantity)
    {
        if (quantity <= 0)
            throw new EntityValidationException("Quantity must be greater than zero.");

        StockQuantity += quantity;
        UpdatedAt = DateTime.UtcNow;
    }

    /// <summary>
    /// Enterprise rule: soft delete.
    /// </summary>
    public void Deactivate()
    {
        Active = false;
        UpdatedAt = DateTime.UtcNow;
    }

    private static void ValidateName(string name)
    {
        if (string.IsNullOrWhiteSpace(name))
            throw new EntityValidationException("Product name is required.");
    }

    private static void ValidatePrice(decimal price)
    {
        if (price <= 0)
            throw new EntityValidationException("Price must be greater than zero.");
    }

    private static void ValidateSku(string sku)
    {
        if (string.IsNullOrWhiteSpace(sku))
            throw new EntityValidationException("SKU is required.");
    }

    private static void ValidateStock(int stock)
    {
        if (stock < 0)
            throw new EntityValidationException("Stock quantity cannot be negative.");
    }
}

/// <summary>
/// Exception for entity-level validation failures (enterprise business rules).
/// </summary>
public class EntityValidationException : Exception
{
    public EntityValidationException(string message) : base(message) { }
}
