package com.ecommerce.paymentservice.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StripeGatewayClientTest {

    private StripeGatewayClient stripeGatewayClient;

    @BeforeEach
    void setUp() {
        stripeGatewayClient = new StripeGatewayClient();
    }

    @Test
    @DisplayName("Should return gateway name as STRIPE")
    void shouldReturnGatewayName() {
        assertThat(stripeGatewayClient.getGatewayName()).isEqualTo("STRIPE");
    }

    @Test
    @DisplayName("Should fail for amounts ending in .99 (test mode)")
    void shouldFailForTestAmounts() {
        // Amounts ending in .99 always fail in simulation
        PaymentGatewayResponse response = stripeGatewayClient.charge(
                "tok_test", new BigDecimal("49.99"), "USD", "idem-123");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("card_declined");
        assertThat(response.getErrorMessage()).contains("declined");
    }

    @Test
    @DisplayName("Should process refund successfully")
    void shouldProcessRefund() {
        PaymentGatewayResponse response = stripeGatewayClient.refund(
                "ch_test123", new BigDecimal("50.00"), "USD");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTransactionId()).startsWith("re_");
        assertThat(response.getRawResponse()).containsKey("id");
        assertThat(response.getRawResponse().get("object")).isEqualTo("refund");
    }

    @Test
    @DisplayName("Should include raw response in charge result")
    void shouldIncludeRawResponseInCharge() {
        // Use amount that doesn't end in .99 for higher success probability
        PaymentGatewayResponse response = stripeGatewayClient.charge(
                "tok_test", new BigDecimal("100.00"), "USD", "idem-456");

        assertThat(response.getRawResponse()).isNotNull();
        assertThat(response.getRawResponse()).isNotEmpty();
    }
}
