package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ReviewRequest;
import com.ecommerce.productservice.dto.ReviewResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductReview;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID productId, ReviewRequest request) {
        log.info("Creating review for product: {} by user: {}", productId, request.getUserId());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (reviewRepository.existsByProductIdAndUserId(productId, request.getUserId())) {
            throw new DuplicateResourceException("Review", "product_id and user_id",
                    productId + " / " + request.getUserId());
        }

        ProductReview review = productMapper.toEntity(request);
        review.setProduct(product);

        ProductReview saved = reviewRepository.save(review);
        log.info("Review created with ID: {}", saved.getId());

        return productMapper.toResponse(saved);
    }

    @Override
    public PagedResponse<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        log.debug("Fetching reviews for product: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        Page<ProductReview> page = reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable);

        return PagedResponse.<ReviewResponse>builder()
                .content(page.getContent().stream()
                        .map(productMapper::toResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse approveReview(UUID reviewId) {
        log.info("Approving review: {}", reviewId);
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        review.setIsApproved(true);
        ProductReview saved = reviewRepository.save(review);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteReview(UUID reviewId) {
        log.info("Deleting review: {}", reviewId);
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
}
