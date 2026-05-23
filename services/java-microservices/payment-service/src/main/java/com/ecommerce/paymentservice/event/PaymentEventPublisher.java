package com.ecommerce.paymentservice.event;

import com.ecommerce.paymentservice.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private static final String TOPIC_PAYMENT_PROCESSED = "payment.processed";
    private static final String TOPIC_PAYMENT_FAILED = "payment.failed";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publishPaymentProcessed(Transaction transaction) {
        PaymentEvent event = buildEvent(transaction, "PAYMENT_PROCESSED");
        publish(TOPIC_PAYMENT_PROCESSED, transaction.getOrderId().toString(), event);
    }

    public void publishPaymentFailed(Transaction transaction) {
        PaymentEvent event = buildEvent(transaction, "PAYMENT_FAILED");
        publish(TOPIC_PAYMENT_FAILED, transaction.getOrderId().toString(), event);
    }

    private void publish(String topic, String key, PaymentEvent event) {
        try {
            kafkaTemplate.send(topic, key, event);
            log.info("Published event to topic={}: eventType={}, transactionId={}",
                    topic, event.getEventType(), event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish event to topic={}: transactionId={}, error={}",
                    topic, event.getTransactionId(), e.getMessage());
        }
    }

    private PaymentEvent buildEvent(Transaction transaction, String eventType) {
        return PaymentEvent.builder()
                .eventType(eventType)
                .transactionId(transaction.getId())
                .transactionNumber(transaction.getTransactionNumber())
                .orderId(transaction.getOrderId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .paymentMethod(transaction.getPaymentMethod().name())
                .paymentGateway(transaction.getPaymentGateway().name())
                .gatewayTransactionId(transaction.getGatewayTransactionId())
                .failureReason(transaction.getFailureReason())
                .processedAt(transaction.getProcessedAt())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
