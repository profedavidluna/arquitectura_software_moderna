package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRequest {

    @NotNull(message = "Actual quantity is required")
    @Min(value = 0, message = "Actual quantity must be >= 0")
    private Integer actualQuantity;

    private String reason;
}
