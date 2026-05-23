package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class PaymentServiceClient {

    private final RestClient restClient;

    public PaymentServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.payment-service.url}") String paymentServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(paymentServiceUrl).build();
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentService")
    public PaymentResponse processPayment(UUID orderId, BigDecimal amount, String currency, String paymentMethod) {
        log.info("Processing payment for order: {}, amount: {} {}", orderId, amount, currency);
        PaymentRequest request = PaymentRequest.builder()
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .build();
        return restClient.post()
                .uri("/api/v1/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "refundPaymentFallback")
    @Retry(name = "paymentService")
    public void refundPayment(UUID orderId) {
        log.info("Refunding payment for order: {}", orderId);
        restClient.post()
                .uri("/api/v1/payments/refund/{orderId}", orderId)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused")
    private PaymentResponse processPaymentFallback(UUID orderId, BigDecimal amount, String currency, String paymentMethod, Throwable t) {
        log.error("Payment service unavailable for order {}: {}", orderId, t.getMessage());
        throw new ServiceUnavailableException("Payment Service", t);
    }

    @SuppressWarnings("unused")
    private void refundPaymentFallback(UUID orderId, Throwable t) {
        log.error("Failed to refund payment for order {}: {}", orderId, t.getMessage());
        throw new ServiceUnavailableException("Payment Service", t);
    }
}
