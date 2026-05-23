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
 * Kafka consumer for user-related events.
 * Listens to: user.registered, user.updated, user.login
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.registered", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserRegistered(@Payload Map<String, Object> message) {
        log.info("Received user.registered event: {}", message);
        try {
            analyticsService.processEvent("user.registered", "user-service", message);
        } catch (Exception e) {
            log.error("Error processing user.registered event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserUpdated(@Payload Map<String, Object> message) {
        log.info("Received user.updated event: {}", message);
        try {
            analyticsService.processEvent("user.updated", "user-service", message);
        } catch (Exception e) {
            log.error("Error processing user.updated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user.login", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserLogin(@Payload Map<String, Object> message) {
        log.info("Received user.login event: {}", message);
        try {
            analyticsService.processEvent("user.login", "user-service", message);
        } catch (Exception e) {
            log.error("Error processing user.login event: {}", e.getMessage(), e);
        }
    }
}
