package com.ecommerce.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private String warehouseLocation;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer totalQuantity;
    private Integer reorderPoint;
    private Integer reorderQuantity;
    private Integer maxQuantity;
    private String stockStatus;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
