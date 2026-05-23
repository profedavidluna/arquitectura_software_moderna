package com.ecommerce.paymentservice.gateway;

import java.math.BigDecimal;

/**
 * Abstraction for payment gateway integrations.
 * Implementations simulate real gateway behavior (Stripe, PayPal, etc.)
 */
public interface PaymentGatewayClient {

    /**
     * Process a payment charge.
     */
    PaymentGatewayResponse charge(String token, BigDecimal amount, String currency, String idempotencyKey);

    /**
     * Process a refund for a previous charge.
     */
    PaymentGatewayResponse refund(String gatewayTransactionId, BigDecimal amount, String currency);

    /**
     * Get the gateway name.
     */
    String getGatewayName();
}
