package com.ecommerce.hexagonal.domain.port.input;

import com.ecommerce.hexagonal.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input Port - defines what the application CAN DO.
 * This is the primary port that driving adapters (controllers) use.
 * No framework dependencies here.
 */
public interface ProductServicePort {

    Product createProduct(String name, String description, BigDecimal price,
                          String category, Integer stockQuantity, String sku);

    Optional<Product> getProductById(UUID id);

    List<Product> getAllProducts(int page, int size);

    List<Product> searchProducts(String query, String category, BigDecimal minPrice, BigDecimal maxPrice);

    Product updateProduct(UUID id, String name, String description, BigDecimal price, String category);

    void deleteProduct(UUID id);

    void decreaseStock(UUID productId, int quantity);

    void increaseStock(UUID productId, int quantity);

    long countProducts();
}
