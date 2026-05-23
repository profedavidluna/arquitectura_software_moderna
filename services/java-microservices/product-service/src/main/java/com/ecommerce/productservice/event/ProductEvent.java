package com.ecommerce.productservice.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {

    private UUID productId;
    private String eventType;
    private String name;
    private String sku;
    private BigDecimal price;
    private UUID categoryId;
    private Boolean isActive;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}
