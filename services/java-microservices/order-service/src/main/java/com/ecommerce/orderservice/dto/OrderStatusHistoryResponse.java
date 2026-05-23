package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {

    private UUID id;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private UUID changedBy;
    private String reason;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
