package com.ecommerce.analyticsservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private UUID id;
    private String eventType;
    private String eventSource;
    private UUID userId;
    private String sessionId;
    private UUID orderId;
    private UUID productId;
    private Map<String, Object> eventData;
    private LocalDateTime createdAt;
}
