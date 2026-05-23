package com.ecommerce.clean.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ENTITY LAYER (innermost) - Enterprise Business Rules.
 * Contains the most general and high-level business rules.
 * NO dependencies on any outer layer. NO framework imports.
 * This is the core of Clean Architecture.
 */
public class Product {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private int stockQuantity;
    private String sku;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product(UUID id, String name, String description, BigDecimal price,
                   String category, int stockQuantity, String sku) {
        validate(name, price, stockQuantity);
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.sku = sku;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Business rules embedded in the entity
    public void updateDetails(String name, String description, BigDecimal price, String category) {
        validate(name, price, this.stockQuantity);
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (stockQuantity < quantity) throw new IllegalStateException(
                "Insufficient stock. Available: " + stockQuantity + ", Requested: " + quantity);
        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isInStock() {
        return stockQuantity > 0 && active;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    private void validate(String name, BigDecimal price, int stock) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (name.length() > 255) throw new IllegalArgumentException("Name too long");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be positive");
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
    }

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public int getStockQuantity() { return stockQuantity; }
    public String getSku() { return sku; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // For reconstitution from persistence
    public void setId(UUID id) { this.id = id; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setActive(boolean active) { this.active = active; }
}
