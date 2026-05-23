package com.ecommerce.paymentservice.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayResponse {

    private boolean success;
    private String transactionId;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> rawResponse;
}
