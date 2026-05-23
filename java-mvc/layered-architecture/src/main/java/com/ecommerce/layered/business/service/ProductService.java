package com.ecommerce.layered.business.service;

import com.ecommerce.layered.business.exception.DuplicateSkuException;
import com.ecommerce.layered.business.exception.InsufficientStockException;
import com.ecommerce.layered.business.exception.ProductNotFoundException;
import com.ecommerce.layered.data.entity.ProductEntity;
import com.ecommerce.layered.data.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * BUSINESS LAYER - Service class.
 * Contains business logic and validation rules.
 * Depends on DATA LAYER (ProductRepository).
 * The Presentation layer depends on this.
 *
 * In layered architecture, the service directly uses the data entity.
 * This is simpler but creates tighter coupling between layers.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductEntity createProduct(String name, String description, BigDecimal price,
                                        String category, Integer stockQuantity, String sku) {
        // Business validation
        if (sku != null && productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setStockQuantity(stockQuantity);
        product.setSku(sku);
        product.setActive(true);

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductEntity getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductEntity> getAllProducts(int page, int size) {
        size = Math.min(size, 100); // Max page size
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findByActiveTrue(pageRequest);
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> searchProducts(String query, String category,
                                               BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.search(query, category, minPrice, maxPrice);
    }

    public ProductEntity updateProduct(UUID id, String name, String description,
                                        BigDecimal price, String category) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);

        return productRepository.save(product);
    }

    public void deleteProduct(UUID id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setActive(false);
        productRepository.save(product);
    }

    public void decreaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getStockQuantity(), quantity);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    public void increaseStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }
}
