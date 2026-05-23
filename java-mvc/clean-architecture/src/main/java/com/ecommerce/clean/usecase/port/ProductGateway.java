package com.ecommerce.clean.usecase.port;

import com.ecommerce.clean.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * USE CASE LAYER - Gateway interface (output boundary).
 * Defines what the use case needs from the outer layers.
 * Implemented by the Interface Adapters layer.
 * NO framework dependencies.
 */
public interface ProductGateway {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll(int page, int size);

    List<Product> search(String query, String category, BigDecimal minPrice, BigDecimal maxPrice);

    boolean existsBySku(String sku);

    long count();
}
