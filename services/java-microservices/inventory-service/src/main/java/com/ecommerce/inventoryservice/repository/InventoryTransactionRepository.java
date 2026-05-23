package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryTransaction;
import com.ecommerce.inventoryservice.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    List<InventoryTransaction> findByProductIdAndTransactionType(UUID productId, TransactionType transactionType);

    List<InventoryTransaction> findByReferenceId(UUID referenceId);

    List<InventoryTransaction> findByProductIdAndCreatedAtBetween(
            UUID productId, LocalDateTime start, LocalDateTime end);
}
