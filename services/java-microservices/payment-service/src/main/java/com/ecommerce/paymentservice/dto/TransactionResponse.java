package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.enums.PaymentGateway;
import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import com.ecommerce.paymentservice.entity.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private String transactionNumber;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private PaymentMethodType paymentMethod;
    private PaymentGateway paymentGateway;
    private String gatewayTransactionId;
    private String failureReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
