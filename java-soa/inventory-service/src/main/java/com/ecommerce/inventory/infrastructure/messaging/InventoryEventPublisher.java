package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.inventory.infrastructure.messaging.events.StockReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Inventory Event Publisher - Infrastructure Layer
 * 
 * <p><b>Observer Pattern</b>: Publishes inventory events to Kafka topics.
 * These events drive the order saga forward or trigger compensation.</p>
 * 
 * <p>Topics published:</p>
 * <ul>
 *   <li>stock.reserved - Stock successfully reserved for an order</li>
 *   <li>stock.insufficient - Not enough stock available</li>
 *   <li>stock.released - Reserved stock released (compensation)</li>
 * </ul>
 */
@Component
public class InventoryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private static final String TOPIC_STOCK_RESERVED = "stock.reserved";
    private static final String TOPIC_STOCK_INSUFFICIENT = "stock.insufficient";
    private static final String TOPIC_STOCK_RELEASED = "stock.released";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes stock.reserved event - saga continues (happy path).
     */
    public void publishStockReserved(UUID orderId, UUID productId, int quantity) {
        StockReservedEvent event = StockReservedEvent.create(orderId, productId, quantity);

        String key = orderId.toString();
        log.info("Publishing StockReservedEvent: orderId={}, productId={}, qty={}",
                orderId, productId, quantity);

        kafkaTemplate.send(TOPIC_STOCK_RESERVED, key, event);
    }

    /**
     * Publishes stock.insufficient event - saga compensation triggered.
     */
    public void publishStockInsufficient(UUID orderId, UUID productId,
                                          int requestedQuantity, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "STOCK_INSUFFICIENT");
        event.put("timestamp", java.time.LocalDateTime.now().toString());
        event.put("orderId", orderId.toString());
        event.put("productId", productId.toString());
        event.put("requestedQuantity", requestedQuantity);
        event.put("reason", reason);

        String key = orderId.toString();
        log.warn("Publishing StockInsufficientEvent: orderId={}, productId={}, reason={}",
                orderId, productId, reason);

        kafkaTemplate.send(TOPIC_STOCK_INSUFFICIENT, key, event);
    }

    /**
     * Publishes stock.released event - compensation completed.
     */
    public void publishStockReleased(UUID orderId, UUID productId, int quantity) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "STOCK_RELEASED");
        event.put("timestamp", java.time.LocalDateTime.now().toString());
        event.put("orderId", orderId.toString());
        event.put("productId", productId.toString());
        event.put("quantity", quantity);

        String key = orderId.toString();
        log.info("Publishing StockReleasedEvent: orderId={}, productId={}, qty={}",
                orderId, productId, quantity);

        kafkaTemplate.send(TOPIC_STOCK_RELEASED, key, event);
    }
}
