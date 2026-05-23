package com.ecommerce.paymentservice.gateway;

import com.ecommerce.paymentservice.entity.enums.PaymentGateway;
import com.ecommerce.paymentservice.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final StripeGatewayClient stripeGatewayClient;
    private final PayPalGatewayClient payPalGatewayClient;

    public PaymentGatewayClient getClient(PaymentGateway gateway) {
        return switch (gateway) {
            case STRIPE -> stripeGatewayClient;
            case PAYPAL -> payPalGatewayClient;
            default -> throw new PaymentProcessingException(
                    "Unsupported payment gateway: " + gateway);
        };
    }
}
