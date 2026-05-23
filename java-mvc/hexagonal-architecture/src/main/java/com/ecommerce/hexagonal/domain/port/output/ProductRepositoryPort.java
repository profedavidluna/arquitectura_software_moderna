package com.ecommerce.hexagonal.domain.port.output;

import com.ecommerce.hexagonal.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port - defines what the application NEEDS from the outside world.
 * This is the secondary port that driven adapters (repositories) implement.
 * No framework dependencies here.
 */
public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAll(int page, int size);

    List<Product> search(String query, String category, BigDecimal minPrice, BigDecimal maxPrice);

    void deleteById(UUID id);

    boolean existsBySku(String sku);

    long count();
}
