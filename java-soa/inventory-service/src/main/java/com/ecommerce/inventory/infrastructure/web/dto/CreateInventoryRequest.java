package com.ecommerce.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInventoryRequest(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotBlank(message = "Product name is required")
        String productName,

        @Min(value = 0, message = "Quantity must be >= 0")
        int quantityAvailable
) {}
