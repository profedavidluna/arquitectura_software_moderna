namespace InventoryService.Infrastructure.Web.Dto;

public record InventoryItemDto(Guid Id, Guid ProductId, string ProductName, int Quantity, int ReservedQuantity, int AvailableQuantity, int MinStockLevel, bool IsLowStock);
