package com.ecommerce.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    @NotBlank
    private String paymentId;

    @NotBlank
    private String orderId;

    @NotBlank
    private String userId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String error;

    private Instant failedAt;

    private String email;
}
