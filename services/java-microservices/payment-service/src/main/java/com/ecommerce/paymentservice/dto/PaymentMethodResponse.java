package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.enums.PaymentGateway;
import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodResponse {

    private UUID id;
    private UUID userId;
    private PaymentMethodType methodType;
    private PaymentGateway provider;
    private String lastFour;
    private String cardBrand;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String billingName;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
