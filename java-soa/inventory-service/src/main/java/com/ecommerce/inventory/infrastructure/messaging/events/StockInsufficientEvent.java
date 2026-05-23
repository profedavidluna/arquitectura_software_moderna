package com.ecommerce.inventory.infrastructure.messaging.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class StockInsufficientEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private UUID orderId;
    private UUID productId;
    private int requestedQuantity;
    private int availableQuantity;

    public static StockInsufficientEvent create(UUID orderId, UUID productId, int requested, int available) {
        StockInsufficientEvent event = new StockInsufficientEvent();
        event.eventId = UUID.randomUUID().toString();
        event.eventType = "STOCK_INSUFFICIENT";
        event.timestamp = LocalDateTime.now();
        event.orderId = orderId;
        event.productId = productId;
        event.requestedQuantity = requested;
        event.availableQuantity = available;
        return event;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
}
