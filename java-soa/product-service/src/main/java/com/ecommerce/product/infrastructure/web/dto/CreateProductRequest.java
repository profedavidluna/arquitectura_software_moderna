package com.ecommerce.product.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Create Product Request DTO
 * 
 * <p><b>DTO Pattern</b>: Data Transfer Object that defines the API contract
 * for creating a product. This is separate from the domain model to:</p>
 * <ul>
 *   <li>Control what data is exposed/accepted via the API</li>
 *   <li>Add validation annotations without polluting the domain</li>
 *   <li>Allow API and domain to evolve independently</li>
 * </ul>
 */
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    private String category;

    @NotBlank(message = "SKU is required")
    private String sku;

    public CreateProductRequest() {}

    public CreateProductRequest(String name, String description, BigDecimal price,
                                String category, String sku) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.sku = sku;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
}
