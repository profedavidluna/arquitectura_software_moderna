package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavePaymentMethodRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Method type is required")
    private PaymentMethodType methodType;

    @NotBlank(message = "Token is required")
    private String token;

    @Pattern(regexp = "\\d{4}", message = "Last four must be exactly 4 digits")
    private String lastFour;

    @Size(max = 20, message = "Card brand must not exceed 20 characters")
    private String cardBrand;

    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    @Min(value = 2024, message = "Expiry year must be 2024 or later")
    private Integer expiryYear;

    @Size(max = 200, message = "Billing name must not exceed 200 characters")
    private String billingName;

    private Map<String, Object> billingAddress;

    @Builder.Default
    private Boolean isDefault = false;
}
