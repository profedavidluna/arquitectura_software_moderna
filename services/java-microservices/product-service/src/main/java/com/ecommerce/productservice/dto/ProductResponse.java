package com.ecommerce.productservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private UUID categoryId;
    private String categoryName;
    private String sku;
    private String barcode;
    private BigDecimal weight;
    private String weightUnit;
    private String imageUrl;
    private List<String> images;
    private String[] tags;
    private Map<String, Object> attributes;
    private Boolean isActive;
    private Boolean isFeatured;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
