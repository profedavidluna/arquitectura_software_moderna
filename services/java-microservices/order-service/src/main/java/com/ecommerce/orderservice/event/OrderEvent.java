package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

    private String eventType;
    private UUID orderId;
    private String orderNumber;
    private UUID userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemEvent> items;
    private LocalDateTime timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent {
        private UUID productId;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
