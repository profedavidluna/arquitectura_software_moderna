package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.enums.PaymentGateway;
import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Builder.Default
    private String currency = "USD";

    @NotNull(message = "Payment method is required")
    private PaymentMethodType paymentMethod;

    private PaymentGateway paymentGateway;

    private String idempotencyKey;

    private String ipAddress;

    private UUID paymentMethodId;
}
