package com.ecommerce.paymentservice.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pact consumer contract tests.
 * Verifies the contract between Payment Service (consumer) and Order Service (provider).
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "OrderService")
class PaymentConsumerPactTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Pact(consumer = "PaymentService")
    public V4Pact getOrderDetailsPact(PactDslWithProvider builder) {
        return builder
                .given("an order exists with ID")
                .uponReceiving("a request to get order details for payment")
                .path("/api/v1/orders/" + "550e8400-e29b-41d4-a716-446655440000")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("id", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                        .uuid("userId")
                        .decimalType("totalAmount", 99.99)
                        .stringType("currency", "USD")
                        .stringType("status", "PENDING_PAYMENT")
                )
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "PaymentService")
    public V4Pact updateOrderPaymentStatusPact(PactDslWithProvider builder) {
        return builder
                .given("an order exists and payment is completed")
                .uponReceiving("a request to update order payment status")
                .path("/api/v1/orders/" + "550e8400-e29b-41d4-a716-446655440000" + "/payment-status")
                .method("PUT")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("status", "PAID")
                        .stringType("transactionId", "ch_test123")
                )
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("id", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                        .stringType("paymentStatus", "PAID")
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrderDetailsPact")
    @DisplayName("Should get order details from Order Service")
    void shouldGetOrderDetails(MockServer mockServer) {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                mockServer.getUrl() + "/api/v1/orders/550e8400-e29b-41d4-a716-446655440000",
                String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.getBody()).contains("PENDING_PAYMENT");
    }

    @Test
    @PactTestFor(pactMethod = "updateOrderPaymentStatusPact")
    @DisplayName("Should update order payment status in Order Service")
    void shouldUpdateOrderPaymentStatus(MockServer mockServer) {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                    "status": "PAID",
                    "transactionId": "ch_test123"
                }
                """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/v1/orders/550e8400-e29b-41d4-a716-446655440000/payment-status",
                HttpMethod.PUT,
                request,
                String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PAID");
    }
}
