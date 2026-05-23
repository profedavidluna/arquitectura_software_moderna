package com.ecommerce.layered.presentation.dto;

import com.ecommerce.layered.data.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PRESENTATION LAYER - Response DTO.
 * In layered architecture, the presentation layer knows about the data entity.
 * This is a trade-off: simpler but tighter coupling.
 */
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
    public static ProductResponse fromEntity(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.getStockQuantity(),
                entity.getSku(),
                entity.isActive(),
                entity.getStockQuantity() > 0 && entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
