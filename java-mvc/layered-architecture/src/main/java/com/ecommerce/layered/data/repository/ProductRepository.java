package com.ecommerce.layered.data.repository;

import com.ecommerce.layered.data.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DATA LAYER - Repository interface.
 * Provides data access operations.
 * The Business layer depends directly on this.
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Page<ProductEntity> findByActiveTrue(Pageable pageable);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM ProductEntity p WHERE p.active = true " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<ProductEntity> search(@Param("query") String query,
                               @Param("category") String category,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice);
}
