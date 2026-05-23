package com.ecommerce.paymentservice.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simulated PayPal payment gateway client.
 * In production, this would integrate with the real PayPal API.
 */
@Component
@Slf4j
public class PayPalGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentGatewayResponse charge(String token, BigDecimal amount, String currency, String idempotencyKey) {
        log.info("Processing PayPal charge: amount={}, currency={}", amount, currency);

        String captureId = "PAYPAL-" + UUID.randomUUID().toString().substring(0, 17).toUpperCase();

        Map<String, Object> rawResponse = new HashMap<>();
        rawResponse.put("id", captureId);
        rawResponse.put("status", "COMPLETED");
        rawResponse.put("amount", Map.of("value", amount.toString(), "currency_code", currency));

        log.info("PayPal charge successful: captureId={}", captureId);
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionId(captureId)
                .rawResponse(rawResponse)
                .build();
    }

    @Override
    public PaymentGatewayResponse refund(String gatewayTransactionId, BigDecimal amount, String currency) {
        log.info("Processing PayPal refund: captureId={}, amount={}", gatewayTransactionId, amount);

        String refundId = "PAYPAL-RFN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();

        Map<String, Object> rawResponse = new HashMap<>();
        rawResponse.put("id", refundId);
        rawResponse.put("status", "COMPLETED");
        rawResponse.put("amount", Map.of("value", amount.toString(), "currency_code", currency));

        log.info("PayPal refund successful: refundId={}", refundId);
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionId(refundId)
                .rawResponse(rawResponse)
                .build();
    }

    @Override
    public String getGatewayName() {
        return "PAYPAL";
    }
}
