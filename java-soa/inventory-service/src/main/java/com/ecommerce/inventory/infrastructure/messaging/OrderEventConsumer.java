package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.inventory.domain.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Order Event Consumer - Saga Pattern Participant.
 *
 * <p><b>Saga Pattern</b>: This consumer participates in the distributed
 * order creation saga. When an order is created, it attempts to reserve
 * stock. If successful, it publishes stock.reserved. If not, it publishes
 * stock.insufficient, causing the Order Service to cancel the order.</p>
 *
 * <p><b>SOA - Loose Coupling</b>: This service doesn't call the Order Service
 * directly. It communicates exclusively through the ESB (Kafka).</p>
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final InventoryService inventoryService;

    public OrderEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Handles order.created events - attempts to reserve stock.
     */
    @KafkaListener(topics = "order.created", groupId = "inventory-service-group")
    public void handleOrderCreated(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String productId = (String) event.get("productId");
        int quantity = ((Number) event.get("quantity")).intValue();

        log.info("Received order.created: orderId={}, productId={}, quantity={}",
                orderId, productId, quantity);

        try {
            UUID prodId = UUID.fromString(productId);
            // reserveStock handles publishing events internally
            boolean reserved = inventoryService.reserveStock(prodId, quantity, UUID.fromString(orderId));

            if (reserved) {
                log.info("Stock reserved successfully: productId={}, reserved={}", productId, quantity);
            } else {
                log.warn("Could not reserve stock: productId={}, quantity={}", productId, quantity);
            }

        } catch (Exception e) {
            log.error("Error processing order.created: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Handles order.cancelled events - releases reserved stock.
     */
    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service-group")
    public void handleOrderCancelled(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String productId = (String) event.get("productId");
        int quantity = ((Number) event.get("quantity")).intValue();

        log.info("Received order.cancelled: orderId={}, productId={}, quantity={}",
                orderId, productId, quantity);

        try {
            UUID prodId = UUID.fromString(productId);
            // releaseStock handles publishing events internally
            inventoryService.releaseStock(prodId, quantity, UUID.fromString(orderId));
            log.info("Stock released: productId={}, released={}", productId, quantity);

        } catch (Exception e) {
            log.error("Error releasing stock: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }
}
