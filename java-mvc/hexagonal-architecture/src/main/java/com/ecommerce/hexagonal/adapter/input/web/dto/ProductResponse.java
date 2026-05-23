package com.ecommerce.hexagonal.adapter.input.web.dto;

import com.ecommerce.hexagonal.domain.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String category,
        Integer stockQuantity,
        String sku,
        boolean active,
        boolean inStock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse fromDomain(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getStockQuantity(),
                product.getSku(),
                product.isActive(),
                product.isInStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
