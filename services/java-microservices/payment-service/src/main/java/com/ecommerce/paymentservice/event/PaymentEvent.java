package com.ecommerce.paymentservice.event;

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
public class PaymentEvent {

    private String eventType;
    private UUID transactionId;
    private String transactionNumber;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String paymentGateway;
    private String gatewayTransactionId;
    private String failureReason;
    private LocalDateTime processedAt;
    private LocalDateTime timestamp;
}
