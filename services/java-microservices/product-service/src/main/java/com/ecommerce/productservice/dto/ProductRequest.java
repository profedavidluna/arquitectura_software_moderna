package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    @Size(max = 280, message = "Slug must not exceed 280 characters")
    private String slug;

    private String description;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Compare at price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal compareAtPrice;

    @DecimalMin(value = "0.00", message = "Cost price must be non-negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal costPrice;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "SKU must be alphanumeric with hyphens only")
    private String sku;

    @Size(max = 50, message = "Barcode must not exceed 50 characters")
    private String barcode;

    @DecimalMin(value = "0.00", message = "Weight must be non-negative")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal weight;

    @Pattern(regexp = "^(kg|lb|g|oz)$", message = "Weight unit must be kg, lb, g, or oz")
    private String weightUnit;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private List<String> images;

    private String[] tags;

    private Map<String, Object> attributes;

    private Boolean isActive;

    private Boolean isFeatured;
}
