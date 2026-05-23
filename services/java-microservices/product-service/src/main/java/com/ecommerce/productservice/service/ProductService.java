package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(UUID id);

    ProductResponse getProductBySlug(String slug);

    PagedResponse<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    void deleteProduct(UUID id);

    PagedResponse<ProductResponse> searchProducts(String query, Pageable pageable);

    PagedResponse<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable);

    PagedResponse<ProductResponse> filterProducts(UUID categoryId, BigDecimal minPrice,
                                                   BigDecimal maxPrice, Boolean isFeatured,
                                                   String tag, Pageable pageable);

    PagedResponse<ProductResponse> getFeaturedProducts(Pageable pageable);
}
