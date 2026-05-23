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
 * Kafka consumer for payment-related events.
 * Listens to: payment.processed, payment.failed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.processed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentProcessed(@Payload Map<String, Object> message) {
        log.info("Received payment.processed event: {}", message);
        try {
            analyticsService.processEvent("payment.processed", "payment-service", message);
        } catch (Exception e) {
            log.error("Error processing payment.processed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailed(@Payload Map<String, Object> message) {
        log.info("Received payment.failed event: {}", message);
        try {
            analyticsService.processEvent("payment.failed", "payment-service", message);
        } catch (Exception e) {
            log.error("Error processing payment.failed event: {}", e.getMessage(), e);
        }
    }
}
