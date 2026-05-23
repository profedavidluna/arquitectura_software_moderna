namespace HexagonalArchitecture.Domain.Models;

/// <summary>
/// Pure domain model representing a Product in the catalog.
/// This class belongs to the DOMAIN layer and has ZERO framework dependencies.
/// It contains business rules and invariants.
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

    public static Product Create(string name, string description, decimal price, string category, int stockQuantity, string sku)
    {
        if (price <= 0)
            throw new DomainException("Price must be greater than zero.");

        if (stockQuantity < 0)
            throw new DomainException("Stock quantity cannot be negative.");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("Product name is required.");

        if (string.IsNullOrWhiteSpace(sku))
            throw new DomainException("SKU is required.");

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

    public void Update(string name, string description, decimal price, string category, string sku)
    {
        if (price <= 0)
            throw new DomainException("Price must be greater than zero.");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("Product name is required.");

        Name = name;
        Description = description ?? string.Empty;
        Price = price;
        Category = category ?? string.Empty;
        Sku = sku;
        UpdatedAt = DateTime.UtcNow;
    }

    public void DecreaseStock(int quantity)
    {
        if (quantity <= 0)
            throw new DomainException("Quantity must be greater than zero.");

        if (StockQuantity - quantity < 0)
            throw new DomainException("Insufficient stock. Cannot decrease below zero.");

        StockQuantity -= quantity;
        UpdatedAt = DateTime.UtcNow;
    }

    public void IncreaseStock(int quantity)
    {
        if (quantity <= 0)
            throw new DomainException("Quantity must be greater than zero.");

        StockQuantity += quantity;
        UpdatedAt = DateTime.UtcNow;
    }

    public void Deactivate()
    {
        Active = false;
        UpdatedAt = DateTime.UtcNow;
    }

    /// <summary>
    /// Reconstitutes a product from persistence data.
    /// Used by the output adapter to rebuild domain objects from stored entities.
    /// </summary>
    public Product Reconstitute(Guid id, bool active, DateTime createdAt, DateTime updatedAt)
    {
        Id = id;
        Active = active;
        CreatedAt = createdAt;
        UpdatedAt = updatedAt;
        return this;
    }
}

/// <summary>
/// Domain exception for business rule violations.
/// </summary>
public class DomainException : Exception
{
    public DomainException(string message) : base(message) { }
}
