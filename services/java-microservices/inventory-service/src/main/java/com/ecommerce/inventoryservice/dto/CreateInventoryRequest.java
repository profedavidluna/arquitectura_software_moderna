package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String warehouseLocation;

    @NotNull(message = "Quantity available is required")
    @Min(value = 0, message = "Quantity available must be >= 0")
    private Integer quantityAvailable;

    @Min(value = 0, message = "Reorder point must be >= 0")
    private Integer reorderPoint;

    @Positive(message = "Reorder quantity must be > 0")
    private Integer reorderQuantity;

    @Positive(message = "Max quantity must be > 0")
    private Integer maxQuantity;
}
