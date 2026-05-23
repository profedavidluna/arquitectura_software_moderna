package com.ecommerce.hexagonal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model - Product entity.
 * This is a pure domain object with NO framework dependencies.
 * Business rules and invariants live here.
 */
public class Product {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    private String sku;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Private constructor - use factory methods
    private Product() {}

    public static Product create(String name, String description, BigDecimal price,
                                  String category, Integer stockQuantity, String sku) {
        validateName(name);
        validatePrice(price);
        validateStock(stockQuantity);

        Product product = new Product();
        product.id = UUID.randomUUID();
        product.name = name;
        product.description = description;
        product.price = price;
        product.category = category;
        product.stockQuantity = stockQuantity;
        product.sku = sku;
        product.active = true;
        product.createdAt = LocalDateTime.now();
        product.updatedAt = LocalDateTime.now();
        return product;
    }

    public static Product reconstitute(UUID id, String name, String description, BigDecimal price,
                                        String category, Integer stockQuantity, String sku,
                                        boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Product product = new Product();
        product.id = id;
        product.name = name;
        product.description = description;
        product.price = price;
        product.category = category;
        product.stockQuantity = stockQuantity;
        product.sku = sku;
        product.active = active;
        product.createdAt = createdAt;
        product.updatedAt = updatedAt;
        return product;
    }

    // Business logic
    public void updateDetails(String name, String description, BigDecimal price, String category) {
        validateName(name);
        validatePrice(price);
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock. Available: " + this.stockQuantity);
        }
        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isInStock() {
        return this.stockQuantity > 0 && this.active;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    // Validation methods (business rules)
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Product name cannot exceed 255 characters");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
    }

    private static void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
    }

    // Getters (no setters - immutability through business methods)
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getSku() { return sku; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
