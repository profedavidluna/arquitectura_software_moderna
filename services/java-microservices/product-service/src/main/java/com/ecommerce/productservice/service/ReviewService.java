package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ReviewRequest;
import com.ecommerce.productservice.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID productId, ReviewRequest request);

    PagedResponse<ReviewResponse> getProductReviews(UUID productId, Pageable pageable);

    ReviewResponse approveReview(UUID reviewId);

    void deleteReview(UUID reviewId);
}
