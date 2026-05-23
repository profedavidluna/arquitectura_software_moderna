using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CleanArchitecture.Framework.Persistence;

/// <summary>
/// EF Core entity (persistence model) for Product.
/// This belongs to the FRAMEWORK layer (outermost) — it is a detail of the persistence mechanism.
/// Named "JpaEntity" to parallel the Java implementation naming convention.
/// </summary>
[Table("products")]
public class ProductJpaEntity
{
    [Key]
    public Guid Id { get; set; }

    [Required]
    [MaxLength(255)]
    public string Name { get; set; } = string.Empty;

    [MaxLength(2000)]
    public string Description { get; set; } = string.Empty;

    [Column(TypeName = "decimal(18,2)")]
    public decimal Price { get; set; }

    [MaxLength(100)]
    public string Category { get; set; } = string.Empty;

    public int StockQuantity { get; set; }

    [Required]
    [MaxLength(50)]
    public string Sku { get; set; } = string.Empty;

    public bool Active { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
