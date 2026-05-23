package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.*;
import com.ecommerce.inventoryservice.event.InventoryEventPublisher;
import com.ecommerce.inventoryservice.exception.DuplicateInventoryException;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.mapper.InventoryMapper;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import com.ecommerce.inventoryservice.repository.LowStockAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final LowStockAlertRepository alertRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        Inventory inventory = findByProductId(productId);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toResponse);
    }

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new DuplicateInventoryException(request.getProductId());
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        inventory = inventoryRepository.save(inventory);

        log.info("Created inventory record for product: {}, SKU: {}", request.getProductId(), request.getSku());
        checkAndCreateLowStockAlert(inventory);

        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse updateInventory(UUID productId, UpdateInventoryRequest request) {
        Inventory inventory = findByProductId(productId);
        inventoryMapper.updateEntity(request, inventory);
        inventory = inventoryRepository.save(inventory);

        log.info("Updated inventory for product: {}", productId);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse reserveStock(UUID productId, ReserveStockRequest request) {
        Inventory inventory = findByProductId(productId);

        if (inventory.getQuantityAvailable() < request.getQuantity()) {
            throw new InsufficientStockException(productId, request.getQuantity(), inventory.getQuantityAvailable());
        }

        int quantityBefore = inventory.getQuantityAvailable();
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - request.getQuantity());
        inventory.setQuantityReserved(inventory.getQuantityReserved() + request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        // Record transaction
        recordTransaction(productId, TransactionType.RESERVE, request.getQuantity(),
                quantityBefore, inventory.getQuantityAvailable(),
                request.getReferenceId(), ReferenceType.ORDER, request.getReason());

        // Publish event
        eventPublisher.publishReserved(productId, inventory.getSku(), request.getQuantity(),
                inventory.getQuantityAvailable(), inventory.getQuantityReserved(), request.getReferenceId());

        // Check low stock
        checkAndCreateLowStockAlert(inventory);

        log.info("Reserved {} units for product: {}", request.getQuantity(), productId);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse releaseStock(UUID productId, ReleaseStockRequest request) {
        Inventory inventory = findByProductId(productId);

        int releaseQty = Math.min(request.getQuantity(), inventory.getQuantityReserved());
        if (releaseQty <= 0) {
            throw new IllegalArgumentException("No reserved stock to release for product: " + productId);
        }

        int quantityBefore = inventory.getQuantityAvailable();
        inventory.setQuantityReserved(inventory.getQuantityReserved() - releaseQty);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + releaseQty);
        inventory = inventoryRepository.save(inventory);

        // Record transaction
        recordTransaction(productId, TransactionType.RELEASE, releaseQty,
                quantityBefore, inventory.getQuantityAvailable(),
                request.getReferenceId(), ReferenceType.ORDER, request.getReason());

        // Resolve alerts if stock is above reorder point
        resolveAlertsIfNeeded(inventory);

        log.info("Released {} units for product: {}", releaseQty, productId);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse depleteStock(UUID productId, DepleteStockRequest request) {
        Inventory inventory = findByProductId(productId);

        if (inventory.getQuantityReserved() < request.getQuantity()) {
            throw new InsufficientStockException(productId, request.getQuantity(), inventory.getQuantityReserved());
        }

        int quantityBefore = inventory.getQuantityAvailable();
        inventory.setQuantityReserved(inventory.getQuantityReserved() - request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        // Record transaction
        recordTransaction(productId, TransactionType.DEPLETE, request.getQuantity(),
                quantityBefore, inventory.getQuantityAvailable(),
                request.getReferenceId(), ReferenceType.ORDER, request.getReason());

        // Publish event
        eventPublisher.publishDepleted(productId, inventory.getSku(), request.getQuantity(),
                inventory.getQuantityAvailable(), inventory.getQuantityReserved(), request.getReferenceId());

        log.info("Depleted {} units for product: {}", request.getQuantity(), productId);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse restockInventory(UUID productId, RestockRequest request) {
        Inventory inventory = findByProductId(productId);

        // Validate max quantity constraint
        if (inventory.getMaxQuantity() != null) {
            int totalAfterRestock = inventory.getQuantityAvailable() + inventory.getQuantityReserved() + request.getQuantity();
            if (totalAfterRestock > inventory.getMaxQuantity()) {
                throw new IllegalArgumentException(
                        String.format("Restock would exceed max quantity (%d). Current total: %d, restock: %d",
                                inventory.getMaxQuantity(),
                                inventory.getQuantityAvailable() + inventory.getQuantityReserved(),
                                request.getQuantity()));
            }
        }

        int quantityBefore = inventory.getQuantityAvailable();
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + request.getQuantity());
        inventory.setLastRestockedAt(LocalDateTime.now());
        inventory = inventoryRepository.save(inventory);

        // Record transaction
        recordTransaction(productId, TransactionType.RESTOCK, request.getQuantity(),
                quantityBefore, inventory.getQuantityAvailable(),
                request.getReferenceId(), ReferenceType.MANUAL, request.getReason());

        // Resolve alerts if stock is above reorder point
        resolveAlertsIfNeeded(inventory);

        log.info("Restocked {} units for product: {}", request.getQuantity(), productId);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public boolean checkAvailability(UUID productId, int quantity) {
        Inventory inventory = findByProductId(productId);
        return inventory.getQuantityAvailable() >= quantity;
    }

    @Transactional
    public InventoryResponse reconcile(UUID productId, ReconciliationRequest request) {
        Inventory inventory = findByProductId(productId);

        int expectedQuantity = inventory.getQuantityAvailable();
        int actualQuantity = request.getActualQuantity();
        int adjustment = actualQuantity - expectedQuantity;

        if (adjustment == 0) {
            log.info("No adjustment needed for product: {}", productId);
            return inventoryMapper.toResponse(inventory);
        }

        inventory.setQuantityAvailable(actualQuantity);
        inventory = inventoryRepository.save(inventory);

        // Record adjustment transaction
        String reason = request.getReason() != null ? request.getReason() :
                String.format("Reconciliation: expected=%d, actual=%d", expectedQuantity, actualQuantity);

        recordTransaction(productId, TransactionType.ADJUSTMENT, adjustment,
                expectedQuantity, actualQuantity, null, ReferenceType.SYSTEM, reason);

        // Check alerts
        if (actualQuantity <= inventory.getReorderPoint()) {
            checkAndCreateLowStockAlert(inventory);
        } else {
            resolveAlertsIfNeeded(inventory);
        }

        log.info("Reconciled inventory for product: {}, adjustment: {}", productId, adjustment);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getLowStockItems(Pageable pageable) {
        return inventoryRepository.findLowStockItems(pageable)
                .map(inventoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(UUID productId, Pageable pageable) {
        return transactionRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(inventoryMapper::toTransactionResponse);
    }

    @Transactional(readOnly = true)
    public Page<LowStockAlertResponse> getActiveAlerts(Pageable pageable) {
        List<AlertStatus> activeStatuses = List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);
        return alertRepository.findByAlertStatusIn(activeStatuses, pageable)
                .map(inventoryMapper::toAlertResponse);
    }

    // --- Private helpers ---

    private Inventory findByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private void recordTransaction(UUID productId, TransactionType type, int quantity,
                                   int quantityBefore, int quantityAfter,
                                   UUID referenceId, ReferenceType referenceType, String reason) {
        InventoryTransaction transaction = InventoryTransaction.builder()
                .productId(productId)
                .transactionType(type)
                .quantity(quantity)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .reason(reason)
                .build();

        transactionRepository.save(transaction);
    }

    private void checkAndCreateLowStockAlert(Inventory inventory) {
        if (inventory.getQuantityAvailable() <= inventory.getReorderPoint()) {
            boolean alertExists = alertRepository.existsByProductIdAndAlertStatus(
                    inventory.getProductId(), AlertStatus.ACTIVE);

            if (!alertExists) {
                LowStockAlert alert = LowStockAlert.builder()
                        .productId(inventory.getProductId())
                        .sku(inventory.getSku())
                        .threshold(inventory.getReorderPoint())
                        .currentQuantity(inventory.getQuantityAvailable())
                        .alertStatus(AlertStatus.ACTIVE)
                        .build();

                alertRepository.save(alert);
                log.warn("Low stock alert created for product: {}, quantity: {}",
                        inventory.getProductId(), inventory.getQuantityAvailable());
            }
        }
    }

    private void resolveAlertsIfNeeded(Inventory inventory) {
        if (inventory.getQuantityAvailable() > inventory.getReorderPoint()) {
            alertRepository.findByProductIdAndAlertStatus(inventory.getProductId(), AlertStatus.ACTIVE)
                    .ifPresent(alert -> {
                        alert.setAlertStatus(AlertStatus.RESOLVED);
                        alert.setResolvedAt(LocalDateTime.now());
                        alertRepository.save(alert);
                        log.info("Resolved low stock alert for product: {}", inventory.getProductId());
                    });
        }
    }
}
