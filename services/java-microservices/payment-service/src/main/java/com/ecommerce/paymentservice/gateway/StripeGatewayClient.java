package com.ecommerce.paymentservice.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simulated Stripe payment gateway client.
 * In production, this would integrate with the real Stripe API.
 */
@Component
@Slf4j
public class StripeGatewayClient implements PaymentGatewayClient {

    private static final double SIMULATED_SUCCESS_RATE = 0.90;

    @Override
    public PaymentGatewayResponse charge(String token, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("Processing Stripe charge: amount={}, currency={}", amount, currency);

        // Simulate gateway processing
        if (shouldSimulateSuccess(amount)) {
            String gatewayTxnId = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

            Map<String, Object> rawResponse = new HashMap<>();
            rawResponse.put("id", gatewayTxnId);
            rawResponse.put("object", "charge");
            rawResponse.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            rawResponse.put("currency", currency.toLowerCase());
            rawResponse.put("status", "succeeded");
            rawResponse.put("paid", true);

            log.info("Stripe charge successful: gatewayTxnId={}", gatewayTxnId);
            return PaymentGatewayResponse.builder()
                    .success(true)
                    .transactionId(gatewayTxnId)
                    .rawResponse(rawResponse)
                    .build();
        } else {
            Map<String, Object> rawResponse = new HashMap<>();
            rawResponse.put("error", Map.of(
                    "type", "card_error",
                    "code", "card_declined",
                    "message", "Your card was declined."
            ));

            log.warn("Stripe charge failed: card_declined");
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .errorCode("card_declined")
                    .errorMessage("Your card was declined.")
                    .rawResponse(rawResponse)
                    .build();
        }
    }

    @Override
    public PaymentGatewayResponse refund(String gatewayTransactionId, BigDecimal amount, String currency) {
        log.info("Processing Stripe refund: gatewayTxnId={}, amount={}", gatewayTransactionId, amount);

        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        Map<String, Object> rawResponse = new HashMap<>();
        rawResponse.put("id", refundId);
        rawResponse.put("object", "refund");
        rawResponse.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
        rawResponse.put("currency", currency.toLowerCase());
        rawResponse.put("status", "succeeded");
        rawResponse.put("charge", gatewayTransactionId);

        log.info("Stripe refund successful: refundId={}", refundId);
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionId(refundId)
                .rawResponse(rawResponse)
                .build();
    }

    @Override
    public String getGatewayName() {
        return "STRIPE";
    }

    private boolean shouldSimulateSuccess(BigDecimal amount) {
        // Amounts ending in .99 always fail (for testing)
        if (amount.remainder(BigDecimal.ONE).compareTo(new BigDecimal("0.99")) == 0) {
            return false;
        }
        return Math.random() < SIMULATED_SUCCESS_RATE;
    }
}
