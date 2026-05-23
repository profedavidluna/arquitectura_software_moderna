package com.ecommerce.product.infrastructure.messaging.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Product Created Event - Message Contract
 * 
 * <p><b>SOA - Service Contract</b>: This event defines the message contract
 * published to the Enterprise Service Bus (Kafka). Other services that subscribe
 * to the "product.created" topic will receive this event.</p>
 * 
 * <p><b>Observer Pattern</b>: This event is the "notification" sent to all
 * observers (subscribers) when a product is created. The publisher doesn't
 * know or care who consumes this event.</p>
 * 
 * <p><b>Factory Pattern</b>: The static factory method ensures consistent
 * event creation with all required fields.</p>
 */
public class ProductCreatedEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private ProductPayload payload;

    public ProductCreatedEvent() {}

    /**
     * Factory method to create a ProductCreatedEvent.
     * Ensures all events have consistent structure.
     */
    public static ProductCreatedEvent create(UUID productId, String name,
                                              String description, BigDecimal price,
                                              String category, String sku) {
        ProductCreatedEvent event = new ProductCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("PRODUCT_CREATED");
        event.setTimestamp(LocalDateTime.now());

        ProductPayload payload = new ProductPayload();
        payload.setProductId(productId.toString());
        payload.setName(name);
        payload.setDescription(description);
        payload.setPrice(price);
        payload.setCategory(category);
        payload.setSku(sku);
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

    public ProductPayload getPayload() { return payload; }
    public void setPayload(ProductPayload payload) { this.payload = payload; }

    /**
     * Nested payload class containing product-specific data.
     */
    public static class ProductPayload {
        private String productId;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String sku;

        public ProductPayload() {}

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
    }
}
