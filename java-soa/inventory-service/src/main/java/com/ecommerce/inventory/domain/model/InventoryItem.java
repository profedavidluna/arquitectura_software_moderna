package com.ecommerce.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inventory Item Domain Model
 * 
 * <p>Represents the stock level for a specific product.
 * Contains business rules for stock reservation and release.</p>
 * 
 * <p><b>Saga Pattern - Participant</b>: This model implements the
 * reservation logic that is part of the order saga. It supports:</p>
 * <ul>
 *   <li>Reserve: Move quantity from available to reserved</li>
 *   <li>Release: Move quantity from reserved back to available (compensation)</li>
 *   <li>Confirm: Remove from reserved (stock is consumed)</li>
 * </ul>
 * 
 * <p><b>SOLID - SRP</b>: This class only manages inventory state.
 * It doesn't know about orders, messaging, or persistence.</p>
 */
public class InventoryItem {

    private UUID id;
    private UUID productId;
    private String productName;
    private int quantityAvailable;
    private int quantityReserved;
    private LocalDateTime updatedAt;

    public InventoryItem() {
        this.id = UUID.randomUUID();
        this.quantityAvailable = 0;
        this.quantityReserved = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public InventoryItem(UUID id, UUID productId, String productName,
                          int quantityAvailable, int quantityReserved,
                          LocalDateTime updatedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantityAvailable = quantityAvailable;
        this.quantityReserved = quantityReserved;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method to create a new inventory entry.
     */
    public static InventoryItem create(UUID productId, String productName, int initialQuantity) {
        InventoryItem item = new InventoryItem();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setQuantityAvailable(initialQuantity);
        return item;
    }

    /**
     * Business rule: Check if sufficient stock is available.
     */
    public boolean hasAvailableStock(int requestedQuantity) {
        return this.quantityAvailable >= requestedQuantity;
    }

    /**
     * Business rule: Reserve stock for an order.
     * <p><b>Saga Pattern</b>: This is the "forward" action in the saga.
     * Stock moves from available to reserved.</p>
     *
     * @param quantity the quantity to reserve
     * @throws IllegalStateException if insufficient stock
     */
    public void reserveStock(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            productId, quantityAvailable, quantity));
        }
        this.quantityAvailable -= quantity;
        this.quantityReserved += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business rule: Release reserved stock (compensation action).
     * <p><b>Saga Pattern</b>: This is the "compensating" action.
     * Called when an order is cancelled after stock was reserved.</p>
     *
     * @param quantity the quantity to release
     */
    public void releaseStock(int quantity) {
        if (this.quantityReserved < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot release %d units. Only %d reserved for product %s",
                            quantity, quantityReserved, productId));
        }
        this.quantityReserved -= quantity;
        this.quantityAvailable += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business rule: Add stock (e.g., from a shipment).
     */
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantityAvailable += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }

    public int getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(int quantityReserved) { this.quantityReserved = quantityReserved; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
