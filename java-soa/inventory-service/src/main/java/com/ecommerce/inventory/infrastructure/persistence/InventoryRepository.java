package com.ecommerce.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Inventory Repository - Spring Data JPA
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {

    /**
     * Find inventory by product ID.
     */
    Optional<InventoryEntity> findByProductId(UUID productId);

    /**
     * Check if inventory exists for a product.
     */
    boolean existsByProductId(UUID productId);
}
