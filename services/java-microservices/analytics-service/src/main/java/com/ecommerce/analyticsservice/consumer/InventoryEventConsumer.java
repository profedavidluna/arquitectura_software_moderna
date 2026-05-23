package com.ecommerce.analyticsservice.consumer;

import com.ecommerce.analyticsservice.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for inventory-related events.
 * Listens to: inventory.reserved, inventory.released, inventory.low-stock
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.reserved", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryReserved(@Payload Map<String, Object> message) {
        log.info("Received inventory.reserved event: {}", message);
        try {
            analyticsService.processEvent("inventory.reserved", "inventory-service", message);
        } catch (Exception e) {
            log.error("Error processing inventory.reserved event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "inventory.released", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryReleased(@Payload Map<String, Object> message) {
        log.info("Received inventory.released event: {}", message);
        try {
            analyticsService.processEvent("inventory.released", "inventory-service", message);
        } catch (Exception e) {
            log.error("Error processing inventory.released event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "inventory.low-stock", groupId = "${spring.kafka.consumer.group-id}")
    public void handleLowStock(@Payload Map<String, Object> message) {
        log.info("Received inventory.low-stock event: {}", message);
        try {
            analyticsService.processEvent("inventory.low-stock", "inventory-service", message);
        } catch (Exception e) {
            log.error("Error processing inventory.low-stock event: {}", e.getMessage(), e);
        }
    }
}
