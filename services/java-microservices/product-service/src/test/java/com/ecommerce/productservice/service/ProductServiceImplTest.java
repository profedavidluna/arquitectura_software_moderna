package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.CategoryRepository;
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
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Product testProduct;
    private ProductRequest testRequest;
    private ProductResponse testResponse;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .slug("electronics")
                .isActive(true)
                .build();

        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .slug("test-product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .sku("TEST-001")
                .category(testCategory)
                .isActive(true)
                .isFeatured(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = ProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("29.99"))
                .sku("TEST-001")
                .categoryId(testCategory.getId())
                .build();

        testResponse = ProductResponse.builder()
                .id(testProduct.getId())
                .name("Test Product")
                .slug("test-product")
                .price(new BigDecimal("29.99"))
                .sku("TEST-001")
                .categoryId(testCategory.getId())
                .categoryName("Electronics")
                .isActive(true)
                .averageRating(0.0)
                .reviewCount(0L)
                .build();
    }

    @Nested
    @DisplayName("Create Product")
    class CreateProduct {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProductSuccessfully() {
            when(productRepository.existsBySku("TEST-001")).thenReturn(false);
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
            when(productMapper.toEntity(testRequest)).thenReturn(testProduct);
            when(productRepository.existsBySlug("test-product")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(null);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(null);

            ProductResponse result = productService.createProduct(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Product");
            assertThat(result.getSku()).isEqualTo("TEST-001");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when SKU already exists")
        void shouldThrowExceptionWhenSkuExists() {
            when(productRepository.existsBySku("TEST-001")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(testRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("sku");
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(productRepository.existsBySku("TEST-001")).thenReturn(false);
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(testRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category");
        }
    }

    @Nested
    @DisplayName("Get Product")
    class GetProduct {

        @Test
        @DisplayName("Should get product by ID")
        void shouldGetProductById() {
            when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(4.5);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(10L);

            ProductResponse result = productService.getProductById(testProduct.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testProduct.getId());
            assertThat(result.getAverageRating()).isEqualTo(4.5);
            assertThat(result.getReviewCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }
    }

    @Nested
    @DisplayName("List Products")
    class ListProducts {

        @Test
        @DisplayName("Should return paginated products")
        void shouldReturnPaginatedProducts() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(productRepository.findByIsActiveTrue(pageable)).thenReturn(page);
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(null);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(null);

            PagedResponse<ProductResponse> result = productService.getAllProducts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Update Product")
    class UpdateProduct {

        @Test
        @DisplayName("Should update product successfully")
        void shouldUpdateProductSuccessfully() {
            when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(null);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(null);

            ProductResponse result = productService.updateProduct(testProduct.getId(), testRequest);

            assertThat(result).isNotNull();
            verify(productMapper).updateEntity(testRequest, testProduct);
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent product")
        void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
            UUID id = UUID.randomUUID();
            when(productRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(id, testRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Product")
    class DeleteProduct {

        @Test
        @DisplayName("Should soft delete product")
        void shouldSoftDeleteProduct() {
            when(productRepository.existsById(testProduct.getId())).thenReturn(true);

            productService.deleteProduct(testProduct.getId());

            verify(productRepository).softDelete(testProduct.getId());
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent product")
        void shouldThrowExceptionWhenDeletingNonExistentProduct() {
            UUID id = UUID.randomUUID();
            when(productRepository.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Search Products")
    class SearchProducts {

        @Test
        @DisplayName("Should search products by query")
        void shouldSearchProductsByQuery() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(productRepository.searchProducts("test", pageable)).thenReturn(page);
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(null);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(null);

            PagedResponse<ProductResponse> result = productService.searchProducts("test", pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return all products when query is blank")
        void shouldReturnAllProductsWhenQueryIsBlank() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(productRepository.findByIsActiveTrue(pageable)).thenReturn(page);
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(null);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(null);

            PagedResponse<ProductResponse> result = productService.searchProducts("", pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Products By Category")
    class GetProductsByCategory {

        @Test
        @DisplayName("Should get products by category")
        void shouldGetProductsByCategory() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(categoryRepository.existsById(testCategory.getId())).thenReturn(true);
            when(productRepository.findByCategoryIdAndIsActiveTrue(testCategory.getId(), pageable)).thenReturn(page);
            when(productMapper.toResponse(testProduct)).thenReturn(testResponse);
            when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(null);
            when(reviewRepository.getReviewCountByProductId(any())).thenReturn(null);

            PagedResponse<ProductResponse> result = productService.getProductsByCategory(testCategory.getId(), pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            UUID categoryId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);

            when(categoryRepository.existsById(categoryId)).thenReturn(false);

            assertThatThrownBy(() -> productService.getProductsByCategory(categoryId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
