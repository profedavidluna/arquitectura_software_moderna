package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.order.domain.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Inventory Event Consumer - Infrastructure Layer
 * 
 * <p><b>Observer Pattern</b>: This consumer subscribes to inventory events
 * published by the Inventory Service. It reacts to stock reservation results
 * to advance the order saga.</p>
 * 
 * <p><b>Saga Pattern - Steps 3 & 4</b>:</p>
 * <ul>
 *   <li>stock.reserved → Confirm the order (happy path)</li>
 *   <li>stock.insufficient → Cancel the order (compensation path)</li>
 * </ul>
 * 
 * <p><b>SOA - Loose Coupling</b>: The Order Service doesn't call the
 * Inventory Service directly. It publishes an event and waits for a response
 * event. This decouples the services temporally and spatially.</p>
 * 
 * <p><b>SOLID - OCP</b>: New event types can be handled by adding new
 * {@code @KafkaListener} methods without modifying existing ones.</p>
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final OrderService orderService;

    public InventoryEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Handles stock.reserved events from the Inventory Service.
     * 
     * <p><b>Saga Step 3</b>: Stock has been successfully reserved.
     * The order can now be confirmed.</p>
     */
    @KafkaListener(topics = "stock.reserved", groupId = "order-service-group")
    public void handleStockReserved(Map<String, Object> event) {
        log.info("Received stock.reserved event: {}", event.get("eventId"));

        try {
            String orderId = extractOrderId(event);
            log.info("Stock reserved for order: orderId={}", orderId);

            // Advance the saga - confirm the order
            orderService.confirmOrder(UUID.fromString(orderId));

            log.info("Order confirmed after stock reservation: orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing stock.reserved event: {}", e.getMessage(), e);
            // In production: implement dead letter queue or retry mechanism
        }
    }

    /**
     * Handles stock.insufficient events from the Inventory Service.
     * 
     * <p><b>Saga Step 4 (Compensation)</b>: Stock is not available.
     * The order must be cancelled as a compensating action.</p>
     */
    @KafkaListener(topics = "stock.insufficient", groupId = "order-service-group")
    public void handleStockInsufficient(Map<String, Object> event) {
        log.info("Received stock.insufficient event: {}", event.get("eventId"));

        try {
            String orderId = extractOrderId(event);
            log.warn("Insufficient stock for order: orderId={}", orderId);

            // Compensating action - cancel the order
            orderService.cancelOrder(UUID.fromString(orderId));

            log.info("Order cancelled due to insufficient stock: orderId={}", orderId);
        } catch (Exception e) {
            log.error("Error processing stock.insufficient event: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts the order ID from the event payload.
     */
    private String extractOrderId(Map<String, Object> event) {
        Object payload = event.get("payload");
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            return (String) payloadMap.get("orderId");
        }
        // Fallback: orderId might be at root level
        return (String) event.get("orderId");
    }
}
