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
 * Kafka consumer for order-related events.
 * Listens to: order.created, order.confirmed, order.shipped, order.cancelled
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(@Payload Map<String, Object> message) {
        log.info("Received order.created event: {}", message);
        try {
            analyticsService.processEvent("order.created", "order-service", message);
        } catch (Exception e) {
            log.error("Error processing order.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.confirmed", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderConfirmed(@Payload Map<String, Object> message) {
        log.info("Received order.confirmed event: {}", message);
        try {
            analyticsService.processEvent("order.confirmed", "order-service", message);
        } catch (Exception e) {
            log.error("Error processing order.confirmed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.shipped", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderShipped(@Payload Map<String, Object> message) {
        log.info("Received order.shipped event: {}", message);
        try {
            analyticsService.processEvent("order.shipped", "order-service", message);
        } catch (Exception e) {
            log.error("Error processing order.shipped event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCancelled(@Payload Map<String, Object> message) {
        log.info("Received order.cancelled event: {}", message);
        try {
            analyticsService.processEvent("order.cancelled", "order-service", message);
        } catch (Exception e) {
            log.error("Error processing order.cancelled event: {}", e.getMessage(), e);
        }
    }
}
