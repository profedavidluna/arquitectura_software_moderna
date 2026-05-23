package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.repository.ProductReviewRepository;
import com.ecommerce.productservice.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);

        if (product.getSlug() == null || product.getSlug().isBlank()) {
            product.setSlug(generateSlug(request.getName()));
        }

        if (productRepository.existsBySlug(product.getSlug())) {
            throw new DuplicateResourceException("Product", "slug", product.getSlug());
        }

        Product saved = productRepository.save(product);
        log.info("Product created with ID: {}", saved.getId());

        return enrichWithReviewData(productMapper.toResponse(saved));
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(UUID id) {
        log.debug("Fetching product by ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return enrichWithReviewData(productMapper.toResponse(product));
    }

    @Override
    @Cacheable(value = "products", key = "#slug")
    public ProductResponse getProductBySlug(String slug) {
        log.debug("Fetching product by slug: {}", slug);
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return enrichWithReviewData(productMapper.toResponse(product));
    }

    @Override
    public PagedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all active products, page: {}", pageable.getPageNumber());
        Page<Product> page = productRepository.findByIsActiveTrue(pageable);
        return toPagedResponse(page);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                throw new DuplicateResourceException("Product", "sku", request.getSku());
            }
        }

        productMapper.updateEntity(request, product);
        Product saved = productRepository.save(product);
        log.info("Product updated with ID: {}", saved.getId());

        return enrichWithReviewData(productMapper.toResponse(saved));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(UUID id) {
        log.info("Soft deleting product with ID: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.softDelete(id);
        log.info("Product soft deleted with ID: {}", id);
    }

    @Override
    public PagedResponse<ProductResponse> searchProducts(String query, Pageable pageable) {
        log.debug("Searching products with query: '{}'", query);
        if (query == null || query.isBlank()) {
            return getAllProducts(pageable);
        }
        Page<Product> page = productRepository.searchProducts(query.trim(), pageable);
        return toPagedResponse(page);
    }

    @Override
    public PagedResponse<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable) {
        log.debug("Fetching products by category: {}", categoryId);
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        Page<Product> page = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        return toPagedResponse(page);
    }

    @Override
    public PagedResponse<ProductResponse> filterProducts(UUID categoryId, BigDecimal minPrice,
                                                          BigDecimal maxPrice, Boolean isFeatured,
                                                          String tag, Pageable pageable) {
        log.debug("Filtering products - category: {}, price: {}-{}, featured: {}, tag: {}",
                categoryId, minPrice, maxPrice, isFeatured, tag);

        Specification<Product> spec = Specification.where(ProductSpecification.isActive());

        if (categoryId != null) {
            spec = spec.and(ProductSpecification.hasCategory(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));
        }
        if (isFeatured != null && isFeatured) {
            spec = spec.and(ProductSpecification.isFeatured());
        }

        Page<Product> page = productRepository.findAll(spec, pageable);
        return toPagedResponse(page);
    }

    @Override
    public PagedResponse<ProductResponse> getFeaturedProducts(Pageable pageable) {
        log.debug("Fetching featured products");
        Page<Product> page = productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable);
        return toPagedResponse(page);
    }

    private ProductResponse enrichWithReviewData(ProductResponse response) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(response.getId());
        Long reviewCount = reviewRepository.getReviewCountByProductId(response.getId());
        response.setAverageRating(avgRating != null ? avgRating : 0.0);
        response.setReviewCount(reviewCount != null ? reviewCount : 0L);
        return response;
    }

    private PagedResponse<ProductResponse> toPagedResponse(Page<Product> page) {
        return PagedResponse.<ProductResponse>builder()
                .content(page.getContent().stream()
                        .map(productMapper::toResponse)
                        .map(this::enrichWithReviewData)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
