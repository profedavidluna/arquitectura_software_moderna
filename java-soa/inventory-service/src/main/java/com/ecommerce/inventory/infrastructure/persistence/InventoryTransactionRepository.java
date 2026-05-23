package com.ecommerce.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Inventory Transaction Repository
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, UUID> {

    List<InventoryTransactionEntity> findByProductId(UUID productId);

    List<InventoryTransactionEntity> findByReferenceId(UUID referenceId);
}
