package com.ecommerce.cartservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {

    private UUID id;
    private String name;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean active;
}
