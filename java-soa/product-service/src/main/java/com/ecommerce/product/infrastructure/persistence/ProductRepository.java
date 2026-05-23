package com.ecommerce.product.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Product Repository - Spring Data JPA Interface
 * 
 * <p><b>Repository Pattern</b>: Abstracts data access behind a collection-like interface.
 * Spring Data JPA provides the implementation at runtime, reducing boilerplate code.</p>
 * 
 * <p><b>SOLID - ISP</b>: We only define the query methods we actually need,
 * rather than exposing all possible CRUD operations.</p>
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    /**
     * Find all active products.
     */
    List<ProductEntity> findByActiveTrue();

    /**
     * Find products by category (only active ones).
     */
    List<ProductEntity> findByCategoryAndActiveTrue(String category);

    /**
     * Check if a product with the given SKU already exists.
     */
    boolean existsBySku(String sku);
}
