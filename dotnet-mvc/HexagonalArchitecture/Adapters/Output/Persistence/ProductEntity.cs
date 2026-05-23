using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace HexagonalArchitecture.Adapters.Output.Persistence;

/// <summary>
/// EF Core entity representing a Product in the database.
/// This belongs to the OUTPUT ADAPTER layer. It is a persistence concern
/// and is mapped to/from the domain model by the repository adapter.
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

    public bool Active { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime UpdatedAt { get; set; }
}
