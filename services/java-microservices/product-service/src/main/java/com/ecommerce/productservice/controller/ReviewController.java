package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ReviewRequest;
import com.ecommerce.productservice.dto.ReviewResponse;
import com.ecommerce.productservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews", description = "Product review management endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Add a review", description = "Adds a new review for a product")
    @ApiResponse(responseCode = "201", description = "Review created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "User already reviewed this product")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID productId,
            @Valid @RequestBody ReviewRequest request) {
        log.info("POST /api/v1/products/{}/reviews", productId);
        ReviewResponse response = reviewService.createReview(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get product reviews", description = "Retrieves approved reviews for a product")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<PagedResponse<ReviewResponse>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.info("GET /api/v1/products/{}/reviews", productId);
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }
}
