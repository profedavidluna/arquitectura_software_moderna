package com.ecommerce.analyticsservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopSellingProductResponse {

    private UUID productId;
    private Integer totalUnitsSold;
    private BigDecimal totalRevenue;
    private Integer totalPurchases;
    private Integer totalViews;
    private BigDecimal averageRating;
}
