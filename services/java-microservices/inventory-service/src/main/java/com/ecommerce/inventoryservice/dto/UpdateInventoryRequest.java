package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryRequest {

    private String warehouseLocation;

    @Min(value = 0, message = "Reorder point must be >= 0")
    private Integer reorderPoint;

    @Positive(message = "Reorder quantity must be > 0")
    private Integer reorderQuantity;

    @Positive(message = "Max quantity must be > 0")
    private Integer maxQuantity;
}
