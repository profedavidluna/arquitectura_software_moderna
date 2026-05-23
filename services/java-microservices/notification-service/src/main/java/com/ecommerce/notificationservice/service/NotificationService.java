package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.dto.*;
import com.ecommerce.notificationservice.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final RetryableEmailService retryableEmailService;

    private static final int MAX_RETRIES = 3;

    /**
     * Sends order confirmation email when an order is created.
     */
    public void sendOrderConfirmationEmail(OrderCreatedEvent event) {
        log.info("Processing order confirmation notification for orderId={}", event.getOrderId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("totalAmount", event.getTotalAmount());
        variables.put("items", event.getItems());
        variables.put("createdAt", event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now());

        retryableEmailService.sendWithRetry(
                event.getEmail(),
                "Order Confirmation - #" + event.getOrderId(),
                "order-confirmation",
                variables,
                MAX_RETRIES
        );
    }

    /**
     * Sends payment receipt email when payment is processed.
     */
    public void sendPaymentReceiptEmail(PaymentProcessedEvent event) {
        log.info("Processing payment receipt notification for paymentId={}", event.getPaymentId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("paymentId", event.getPaymentId());
        variables.put("orderId", event.getOrderId());
        variables.put("amount", event.getAmount());
        variables.put("paymentMethod", event.getPaymentMethod());
        variables.put("processedAt", event.getProcessedAt() != null ? event.getProcessedAt() : Instant.now());

        retryableEmailService.sendWithRetry(
                event.getEmail(),
                "Payment Receipt - Order #" + event.getOrderId(),
                "payment-receipt",
                variables,
                MAX_RETRIES
        );
    }

    /**
     * Sends shipping notification email when an order is shipped.
     */
    public void sendShippingNotificationEmail(OrderShippedEvent event) {
        log.info("Processing shipping notification for orderId={}", event.getOrderId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("trackingNumber", event.getTrackingNumber());
        variables.put("carrier", event.getCarrier());
        variables.put("shippedAt", event.getShippedAt() != null ? event.getShippedAt() : Instant.now());

        retryableEmailService.sendWithRetry(
                event.getEmail(),
                "Your Order #" + event.getOrderId() + " Has Been Shipped!",
                "shipping-notification",
                variables,
                MAX_RETRIES
        );
    }

    /**
     * Sends order cancellation email.
     */
    public void sendOrderCancellationEmail(OrderCancelledEvent event) {
        log.info("Processing order cancellation notification for orderId={}", event.getOrderId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("reason", event.getReason());
        variables.put("cancelledAt", event.getCancelledAt() != null ? event.getCancelledAt() : Instant.now());

        retryableEmailService.sendWithRetry(
                event.getEmail(),
                "Order #" + event.getOrderId() + " Has Been Cancelled",
                "order-cancellation",
                variables,
                MAX_RETRIES
        );
    }

    /**
     * Sends welcome email when a new user registers.
     */
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        log.info("Processing welcome notification for userId={}", event.getUserId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", event.getUsername());
        variables.put("registeredAt", event.getRegisteredAt() != null ? event.getRegisteredAt() : Instant.now());

        retryableEmailService.sendWithRetry(
                event.getEmail(),
                "Welcome to E-Commerce Platform!",
                "welcome",
                variables,
                MAX_RETRIES
        );
    }

    /**
     * Sends payment failure notification email.
     */
    public void sendPaymentFailureEmail(PaymentFailedEvent event) {
        log.info("Processing payment failure notification for paymentId={}", event.getPaymentId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("paymentId", event.getPaymentId());
        variables.put("orderId", event.getOrderId());
        variables.put("amount", event.getAmount());
        variables.put("error", event.getError());
        variables.put("failedAt", event.getFailedAt() != null ? event.getFailedAt() : Instant.now());

        retryableEmailService.sendWithRetry(
                event.getEmail(),
                "Payment Failed - Order #" + event.getOrderId(),
                "payment-failure",
                variables,
                MAX_RETRIES
        );
    }
}
