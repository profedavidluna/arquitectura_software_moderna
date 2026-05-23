using System.ComponentModel.DataAnnotations;

namespace InventoryService.Infrastructure.Web.Dto;

/// <summary>
/// DTO for inventory creation/update requests.
/// </summary>
public record CreateInventoryRequest
{
    [Required]
    public Guid ProductId { get; init; }

    [Required]
    [StringLength(255)]
    public string ProductName { get; init; } = string.Empty;

    [Required]
    [Range(0, int.MaxValue, ErrorMessage = "Quantity must be non-negative")]
    public int Quantity { get; init; }
}
