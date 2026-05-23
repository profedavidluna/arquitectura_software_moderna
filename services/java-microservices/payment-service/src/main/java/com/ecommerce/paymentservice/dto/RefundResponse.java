package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.enums.ReasonCategory;
import com.ecommerce.paymentservice.entity.enums.RefundStatus;
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
public class RefundResponse {

    private UUID id;
    private String refundNumber;
    private UUID transactionId;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private ReasonCategory reasonCategory;
    private RefundStatus status;
    private String gatewayRefundId;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
