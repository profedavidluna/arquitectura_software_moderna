using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace LayeredArchitecture.Data.Entities;

/// <summary>
/// Entity representing a Product in the database.
/// This belongs to the DATA LAYER — the lowest layer in the layered architecture.
/// In a layered architecture, the entity is shared across layers (Business layer uses it directly).
/// </summary>
[Table("products")]
public class ProductEntity
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

    public bool Active { get; set; } = true;

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
