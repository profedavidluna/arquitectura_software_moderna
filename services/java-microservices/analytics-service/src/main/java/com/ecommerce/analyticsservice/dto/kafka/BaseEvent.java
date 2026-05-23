package com.ecommerce.analyticsservice.dto.kafka;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseEvent {

    private UUID eventId;
    private String eventType;
    private String source;
    private LocalDateTime timestamp;
    private UUID userId;
    private UUID orderId;
    private UUID productId;
    private Map<String, Object> payload;
}
