package com.ecommerce.inventory.infrastructure.messaging.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class StockReleasedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private UUID orderId;
    private UUID productId;
    private int releasedQuantity;

    public static StockReleasedEvent create(UUID orderId, UUID productId, int releasedQuantity) {
        StockReleasedEvent event = new StockReleasedEvent();
        event.eventId = UUID.randomUUID().toString();
        event.eventType = "STOCK_RELEASED";
        event.timestamp = LocalDateTime.now();
        event.orderId = orderId;
        event.productId = productId;
        event.releasedQuantity = releasedQuantity;
        return event;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public int getReleasedQuantity() { return releasedQuantity; }
}
