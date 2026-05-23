namespace InventoryService.Domain.Interfaces;

using InventoryService.Domain.Models;

public interface IInventoryRepository
{
    Task<InventoryItem?> GetByIdAsync(Guid id);
    Task<InventoryItem?> GetByProductIdAsync(Guid productId);
    Task<IEnumerable<InventoryItem>> GetAllAsync();
    Task<InventoryItem> CreateAsync(InventoryItem item);
    Task<InventoryItem> UpdateAsync(InventoryItem item);
    Task<Reservation> CreateReservationAsync(Reservation reservation);
    Task<IEnumerable<Reservation>> GetReservationsByOrderIdAsync(Guid orderId);
    Task<bool> UpdateReservationStatusAsync(Guid orderId, string status);
}

public interface IInventoryService
{
    Task<InventoryItem?> GetByProductIdAsync(Guid productId);
    Task<IEnumerable<InventoryItem>> GetAllAsync();
    Task<InventoryItem> CreateItemAsync(CreateInventoryItemRequest request);
    Task<InventoryItem> UpdateStockAsync(Guid productId, UpdateStockRequest request);
    Task<bool> ReserveStockAsync(ReserveStockRequest request);
    Task<bool> ReleaseReservationAsync(Guid orderId);
    Task<bool> ConfirmReservationAsync(Guid orderId);
}
