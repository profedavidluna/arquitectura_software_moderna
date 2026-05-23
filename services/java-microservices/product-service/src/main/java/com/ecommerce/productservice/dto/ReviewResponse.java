package com.ecommerce.productservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private UUID id;
    private UUID productId;
    private UUID userId;
    private Integer rating;
    private String title;
    private String reviewText;
    private Boolean isVerifiedPurchase;
    private Boolean isApproved;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
