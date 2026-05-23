package com.ecommerce.clean.framework.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * FRAMEWORK LAYER - Spring Data JPA Repository.
 */
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    boolean existsBySku(String sku);

    Page<ProductJpaEntity> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM ProductJpaEntity p WHERE p.active = true " +
           "AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<ProductJpaEntity> search(@Param("query") String query,
                                   @Param("category") String category,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice);
}
