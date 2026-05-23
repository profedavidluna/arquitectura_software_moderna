package com.ecommerce.inventory.infrastructure.messaging.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stock Reserved Event
 * 
 * <p><b>Saga Pattern - Step 2 Success</b>: Published when stock is successfully
 * reserved for an order. The Order Service consumes this to confirm the order.</p>
 * 
 * <p><b>Observer Pattern</b>: Any service interested in stock reservations
 * can subscribe to the "stock.reserved" topic.</p>
 */
public class StockReservedEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private StockPayload payload;

    public StockReservedEvent() {}

    /**
     * Factory method for consistent event creation.
     */
    public static StockReservedEvent create(UUID orderId, UUID productId, int quantity) {
        StockReservedEvent event = new StockReservedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("STOCK_RESERVED");
        event.setTimestamp(LocalDateTime.now());

        StockPayload payload = new StockPayload();
        payload.setOrderId(orderId.toString());
        payload.setProductId(productId.toString());
        payload.setQuantity(quantity);
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

    public StockPayload getPayload() { return payload; }
    public void setPayload(StockPayload payload) { this.payload = payload; }

    /**
     * Stock event payload.
     */
    public static class StockPayload {
        private String orderId;
        private String productId;
        private int quantity;
        private String reason;

        public StockPayload() {}

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
