package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.dto.*;
import com.ecommerce.notificationservice.service.NotificationService;
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

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(@Payload Map<String, Object> message) {
        log.info("Received order.created event: {}", message);
        try {
            OrderCreatedEvent event = objectMapper.convertValue(message, OrderCreatedEvent.class);
            notificationService.sendOrderConfirmationEmail(event);
        } catch (Exception e) {
            log.error("Error processing order.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.confirmed", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderConfirmed(@Payload Map<String, Object> message) {
        log.info("Received order.confirmed event: {}", message);
        try {
            OrderConfirmedEvent event = objectMapper.convertValue(message, OrderConfirmedEvent.class);
            notificationService.sendOrderConfirmationEmail(OrderCreatedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .email(event.getEmail())
                    .createdAt(event.getConfirmedAt())
                    .build());
        } catch (Exception e) {
            log.error("Error processing order.confirmed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.shipped", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderShipped(@Payload Map<String, Object> message) {
        log.info("Received order.shipped event: {}", message);
        try {
            OrderShippedEvent event = objectMapper.convertValue(message, OrderShippedEvent.class);
            notificationService.sendShippingNotificationEmail(event);
        } catch (Exception e) {
            log.error("Error processing order.shipped event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCancelled(@Payload Map<String, Object> message) {
        log.info("Received order.cancelled event: {}", message);
        try {
            OrderCancelledEvent event = objectMapper.convertValue(message, OrderCancelledEvent.class);
            notificationService.sendOrderCancellationEmail(event);
        } catch (Exception e) {
            log.error("Error processing order.cancelled event: {}", e.getMessage(), e);
        }
    }
}
