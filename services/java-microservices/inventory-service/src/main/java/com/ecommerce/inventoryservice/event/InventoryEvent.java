package com.ecommerce.inventoryservice.event;

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
public class InventoryEvent {

    private String eventType;
    private UUID productId;
    private String sku;
    private Integer quantity;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private UUID referenceId;
    private String reason;
    private LocalDateTime timestamp;
}
