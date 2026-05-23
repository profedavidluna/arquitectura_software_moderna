package com.ecommerce.inventory.domain.service;

import com.ecommerce.inventory.domain.model.InventoryItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inventory Service Interface - Domain Layer
 * 
 * <p><b>SOLID - ISP</b>: Focused interface for inventory management operations.</p>
 * 
 * <p><b>SOLID - DIP</b>: Controllers and event consumers depend on this
 * abstraction, not on the concrete implementation.</p>
 * 
 * <p><b>Strategy Pattern</b>: Different reservation strategies could be
 * implemented (FIFO, priority-based, etc.) behind this interface.</p>
 */
public interface InventoryService {

    /**
     * Creates a new inventory entry for a product.
     */
    InventoryItem createInventoryItem(UUID productId, String productName, int initialQuantity);

    /**
     * Retrieves inventory for a specific product.
     */
    Optional<InventoryItem> getInventoryByProductId(UUID productId);

    /**
     * Retrieves all inventory items.
     */
    List<InventoryItem> getAllInventory();

    /**
     * Reserves stock for an order.
     * <p><b>Saga Pattern</b>: Forward action - reserves stock.</p>
     *
     * @param productId the product to reserve
     * @param quantity the quantity to reserve
     * @param orderId the order requesting the reservation
     * @return true if reservation successful, false if insufficient stock
     */
    boolean reserveStock(UUID productId, int quantity, UUID orderId);

    /**
     * Releases previously reserved stock.
     * <p><b>Saga Pattern</b>: Compensating action - releases stock.</p>
     *
     * @param productId the product to release
     * @param quantity the quantity to release
     * @param orderId the order releasing the reservation
     */
    void releaseStock(UUID productId, int quantity, UUID orderId);

    /**
     * Adds stock to inventory (e.g., from a supplier shipment).
     */
    InventoryItem addStock(UUID productId, int quantity);
}
