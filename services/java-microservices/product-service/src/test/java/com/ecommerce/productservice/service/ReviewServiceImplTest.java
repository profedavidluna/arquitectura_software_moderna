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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Product testProduct;
    private ProductReview testReview;
    private ReviewRequest testRequest;
    private ReviewResponse testResponse;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .price(new BigDecimal("29.99"))
                .sku("TEST-001")
                .isActive(true)
                .build();

        testReview = ProductReview.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .userId(userId)
                .rating(5)
                .title("Great product")
                .reviewText("Really enjoyed this product")
                .isVerifiedPurchase(true)
                .isApproved(false)
                .helpfulCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = ReviewRequest.builder()
                .userId(userId)
                .rating(5)
                .title("Great product")
                .reviewText("Really enjoyed this product")
                .isVerifiedPurchase(true)
                .build();

        testResponse = ReviewResponse.builder()
                .id(testReview.getId())
                .productId(productId)
                .userId(userId)
                .rating(5)
                .title("Great product")
                .reviewText("Really enjoyed this product")
                .isVerifiedPurchase(true)
                .isApproved(false)
                .helpfulCount(0)
                .build();
    }

    @Nested
    @DisplayName("Create Review")
    class CreateReview {

        @Test
        @DisplayName("Should create review successfully")
        void shouldCreateReviewSuccessfully() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(false);
            when(productMapper.toEntity(testRequest)).thenReturn(testReview);
            when(reviewRepository.save(any(ProductReview.class))).thenReturn(testReview);
            when(productMapper.toResponse(testReview)).thenReturn(testResponse);

            ReviewResponse result = reviewService.createReview(productId, testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRating()).isEqualTo(5);
            verify(reviewRepository).save(any(ProductReview.class));
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(productId, testRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user already reviewed")
        void shouldThrowExceptionWhenUserAlreadyReviewed() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(productId, testRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("Get Reviews")
    class GetReviews {

        @Test
        @DisplayName("Should get product reviews")
        void shouldGetProductReviews() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProductReview> page = new PageImpl<>(List.of(testReview), pageable, 1);

            when(productRepository.existsById(productId)).thenReturn(true);
            when(reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable)).thenReturn(page);
            when(productMapper.toResponse(testReview)).thenReturn(testResponse);

            PagedResponse<ReviewResponse> result = reviewService.getProductReviews(productId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            Pageable pageable = PageRequest.of(0, 10);
            when(productRepository.existsById(productId)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.getProductReviews(productId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Approve Review")
    class ApproveReview {

        @Test
        @DisplayName("Should approve review")
        void shouldApproveReview() {
            when(reviewRepository.findById(testReview.getId())).thenReturn(Optional.of(testReview));
            when(reviewRepository.save(any(ProductReview.class))).thenReturn(testReview);
            when(productMapper.toResponse(testReview)).thenReturn(testResponse);

            ReviewResponse result = reviewService.approveReview(testReview.getId());

            assertThat(testReview.getIsApproved()).isTrue();
            verify(reviewRepository).save(testReview);
        }
    }
}
