package com.ecommerce.inventoryservice.dto;

import com.ecommerce.inventoryservice.entity.AlertStatus;
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
public class LowStockAlertResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private Integer threshold;
    private Integer currentQuantity;
    private AlertStatus alertStatus;
    private UUID acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
