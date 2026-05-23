package com.ecommerce.inventoryservice.dto;

import com.ecommerce.inventoryservice.entity.ReferenceType;
import com.ecommerce.inventoryservice.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private UUID productId;
    private TransactionType transactionType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private UUID referenceId;
    private ReferenceType referenceType;
    private String reason;
    private UUID performedBy;
    private LocalDateTime createdAt;
}
