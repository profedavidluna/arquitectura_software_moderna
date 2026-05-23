package com.ecommerce.hexagonal.domain.service;

import com.ecommerce.hexagonal.domain.model.Product;
import com.ecommerce.hexagonal.domain.port.input.ProductServicePort;
import com.ecommerce.hexagonal.domain.port.output.ProductRepositoryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain Service - implements the input port.
 * Contains business logic orchestration.
 * Depends ONLY on domain model and output ports (interfaces).
 * NO framework annotations here - Spring config wires this up.
 */
public class ProductService implements ProductServicePort {

    private final ProductRepositoryPort productRepository;

    public ProductService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product createProduct(String name, String description, BigDecimal price,
                                  String category, Integer stockQuantity, String sku) {
        // Business rule: SKU must be unique
        if (sku != null && productRepository.existsBySku(sku)) {
            throw new IllegalArgumentException("Product with SKU '" + sku + "' already exists");
        }

        Product product = Product.create(name, description, price, category, stockQuantity, sku);
        return productRepository.save(product);
    }

    @Override
    public Optional<Product> getProductById(UUID id) {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> getAllProducts(int page, int size) {
        if (size > 100) {
            size = 100; // Business rule: max page size is 100
        }
        return productRepository.findAll(page, size);
    }

    @Override
    public List<Product> searchProducts(String query, String category,
                                         BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.search(query, category, minPrice, maxPrice);
    }

    @Override
    public Product updateProduct(UUID id, String name, String description,
                                  BigDecimal price, String category) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.updateDetails(name, description, price, category);
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.deactivate();
        productRepository.save(product);
    }

    @Override
    public void decreaseStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    @Override
    public void increaseStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        product.increaseStock(quantity);
        productRepository.save(product);
    }

    @Override
    public long countProducts() {
        return productRepository.count();
    }
}
