package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.dto.UserRegisteredEvent;
import com.ecommerce.notificationservice.dto.UserUpdatedEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for user-related events.
 * Listens to: user.registered, user.updated
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.registered", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserRegistered(@Payload Map<String, Object> message) {
        log.info("Received user.registered event: {}", message);
        try {
            UserRegisteredEvent event = objectMapper.convertValue(message, UserRegisteredEvent.class);
            notificationService.sendWelcomeEmail(event);
        } catch (Exception e) {
            log.error("Error processing user.registered event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserUpdated(@Payload Map<String, Object> message) {
        log.info("Received user.updated event: {}", message);
        try {
            UserUpdatedEvent event = objectMapper.convertValue(message, UserUpdatedEvent.class);
            log.info("User profile updated for userId={}, email={}", event.getUserId(), event.getEmail());
            // User update events are logged but no email is sent unless preferences change
        } catch (Exception e) {
            log.error("Error processing user.updated event: {}", e.getMessage(), e);
        }
    }
}
