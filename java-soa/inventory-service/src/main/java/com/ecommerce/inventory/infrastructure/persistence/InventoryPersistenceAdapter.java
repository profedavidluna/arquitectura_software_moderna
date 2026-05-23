package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.domain.model.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Inventory Persistence Adapter - Infrastructure Layer
 * 
 * <p><b>Adapter Pattern</b>: Bridges the domain model with JPA entities.
 * Also handles inventory transaction recording for audit purposes.</p>
 */
@Component
public class InventoryPersistenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(InventoryPersistenceAdapter.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    public InventoryPersistenceAdapter(InventoryRepository inventoryRepository,
                                        InventoryTransactionRepository transactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public InventoryItem save(InventoryItem item) {
        InventoryEntity entity = toEntity(item);
        InventoryEntity saved = inventoryRepository.save(entity);
        log.debug("Inventory persisted: productId={}, available={}, reserved={}",
                saved.getProductId(), saved.getQuantityAvailable(), saved.getQuantityReserved());
        return toDomain(saved);
    }

    public Optional<InventoryItem> findByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId).map(this::toDomain);
    }

    public List<InventoryItem> findAll() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Records an inventory transaction for audit trail.
     */
    public void recordTransaction(UUID productId, String type, int quantity, UUID referenceId) {
        InventoryTransactionEntity transaction = InventoryTransactionEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .transactionType(type)
                .quantity(quantity)
                .referenceId(referenceId)
                .build();

        transactionRepository.save(transaction);
        log.debug("Transaction recorded: type={}, productId={}, qty={}, ref={}",
                type, productId, quantity, referenceId);
    }

    // =========================================================================
    // Mapping Methods
    // =========================================================================

    private InventoryEntity toEntity(InventoryItem item) {
        return InventoryEntity.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantityAvailable(item.getQuantityAvailable())
                .quantityReserved(item.getQuantityReserved())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private InventoryItem toDomain(InventoryEntity entity) {
        return new InventoryItem(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantityAvailable(),
                entity.getQuantityReserved(),
                entity.getUpdatedAt()
        );
    }
}
