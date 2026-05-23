package com.ecommerce.clean.framework.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank String category,
        @NotNull @Min(0) Integer stockQuantity,
        String sku
) {}
