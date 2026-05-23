package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.dto.PaymentFailedEvent;
import com.ecommerce.notificationservice.dto.PaymentProcessedEvent;
import com.ecommerce.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for payment-related events.
 * Listens to: payment.processed, payment.failed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.processed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentProcessed(@Payload Map<String, Object> message) {
        log.info("Received payment.processed event: {}", message);
        try {
            PaymentProcessedEvent event = objectMapper.convertValue(message, PaymentProcessedEvent.class);
            notificationService.sendPaymentReceiptEmail(event);
        } catch (Exception e) {
            log.error("Error processing payment.processed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailed(@Payload Map<String, Object> message) {
        log.info("Received payment.failed event: {}", message);
        try {
            PaymentFailedEvent event = objectMapper.convertValue(message, PaymentFailedEvent.class);
            notificationService.sendPaymentFailureEmail(event);
        } catch (Exception e) {
            log.error("Error processing payment.failed event: {}", e.getMessage(), e);
        }
    }
}
