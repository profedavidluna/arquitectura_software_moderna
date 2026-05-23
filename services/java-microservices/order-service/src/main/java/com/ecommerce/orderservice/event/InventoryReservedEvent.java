package com.ecommerce.orderservice.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent {

    private UUID orderId;
    private boolean reserved;
    private String reason;
    private LocalDateTime timestamp;
}
