package com.ecommerce.order.infrastructure.messaging.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order Created Event - Published when a new order is placed
 * 
 * <p><b>Saga Pattern - Step 1</b>: This event initiates the order saga.
 * The Inventory Service subscribes to this event and attempts to reserve stock.</p>
 * 
 * <p><b>SOA - Message Contract</b>: This defines the contract between
 * the Order Service (publisher) and any subscriber. The contract includes
 * all information needed for downstream processing.</p>
 */
public class OrderCreatedEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private OrderPayload payload;

    public OrderCreatedEvent() {}

    /**
     * Factory method for consistent event creation.
     */
    public static OrderCreatedEvent create(UUID orderId, UUID userId,
                                            BigDecimal totalAmount,
                                            List<OrderItemPayload> items) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("ORDER_CREATED");
        event.setTimestamp(LocalDateTime.now());

        OrderPayload payload = new OrderPayload();
        payload.setOrderId(orderId.toString());
        payload.setUserId(userId.toString());
        payload.setTotalAmount(totalAmount);
        payload.setItems(items);
        event.setPayload(payload);

        return event;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public OrderPayload getPayload() { return payload; }
    public void setPayload(OrderPayload payload) { this.payload = payload; }

    /**
     * Order payload containing order details.
     */
    public static class OrderPayload {
        private String orderId;
        private String userId;
        private BigDecimal totalAmount;
        private List<OrderItemPayload> items;

        public OrderPayload() {}

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public List<OrderItemPayload> getItems() { return items; }
        public void setItems(List<OrderItemPayload> items) { this.items = items; }
    }

    /**
     * Order item payload for the event.
     */
    public static class OrderItemPayload {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;

        public OrderItemPayload() {}

        public OrderItemPayload(String productId, String productName,
                                int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
