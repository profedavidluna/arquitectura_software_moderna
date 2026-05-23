package com.ecommerce.inventory.application;

import com.ecommerce.inventory.domain.model.InventoryItem;
import com.ecommerce.inventory.domain.service.InventoryService;
import com.ecommerce.inventory.infrastructure.messaging.InventoryEventPublisher;
import com.ecommerce.inventory.infrastructure.persistence.InventoryPersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inventory Service Implementation - Application Layer
 * 
 * <p><b>Saga Pattern - Participant</b>: This service participates in the
 * order saga by handling stock reservations and releases. It publishes
 * events to communicate the result back to the Order Service.</p>
 * 
 * <p><b>Strategy Pattern</b>: The reservation strategy is encapsulated here.
 * Currently uses a simple "first come, first served" approach. Could be
 * extended with priority queues or batch reservation strategies.</p>
 * 
 * <p><b>SOLID - SRP</b>: Orchestrates inventory operations between
 * persistence and messaging layers.</p>
 */
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryPersistenceAdapter persistenceAdapter;
    private final InventoryEventPublisher eventPublisher;

    public InventoryServiceImpl(InventoryPersistenceAdapter persistenceAdapter,
                                 InventoryEventPublisher eventPublisher) {
        this.persistenceAdapter = persistenceAdapter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public InventoryItem createInventoryItem(UUID productId, String productName, int initialQuantity) {
        log.info("Creating inventory item: productId={}, name={}, qty={}",
                productId, productName, initialQuantity);

        InventoryItem item = InventoryItem.create(productId, productName, initialQuantity);
        InventoryItem saved = persistenceAdapter.save(item);

        log.info("Inventory item created: id={}, productId={}", saved.getId(), saved.getProductId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryItem> getInventoryByProductId(UUID productId) {
        return persistenceAdapter.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllInventory() {
        return persistenceAdapter.findAll();
    }

    /**
     * Reserves stock for an order - Saga forward action.
     * 
     * <p><b>Saga Step 2</b>: Called when an order.created event is received.
     * Attempts to reserve the requested quantity. Publishes either
     * stock.reserved or stock.insufficient based on availability.</p>
     * 
     * @return true if stock was successfully reserved
     */
    @Override
    public boolean reserveStock(UUID productId, int quantity, UUID orderId) {
        log.info("Attempting to reserve stock: productId={}, qty={}, orderId={}",
                productId, quantity, orderId);

        Optional<InventoryItem> optItem = persistenceAdapter.findByProductId(productId);

        if (optItem.isEmpty()) {
            log.warn("Product not found in inventory: productId={}", productId);
            eventPublisher.publishStockInsufficient(orderId, productId, quantity,
                    "Product not found in inventory");
            return false;
        }

        InventoryItem item = optItem.get();

        if (!item.hasAvailableStock(quantity)) {
            log.warn("Insufficient stock: productId={}, available={}, requested={}",
                    productId, item.getQuantityAvailable(), quantity);
            eventPublisher.publishStockInsufficient(orderId, productId, quantity,
                    "Insufficient stock available");
            return false;
        }

        // Reserve the stock
        item.reserveStock(quantity);
        persistenceAdapter.save(item);

        // Record the transaction
        persistenceAdapter.recordTransaction(productId, "RESERVE", quantity, orderId);

        // Publish success event
        eventPublisher.publishStockReserved(orderId, productId, quantity);

        log.info("Stock reserved successfully: productId={}, qty={}, orderId={}",
                productId, quantity, orderId);
        return true;
    }

    /**
     * Releases reserved stock - Saga compensating action.
     * 
     * <p><b>Saga Compensation</b>: Called when an order.cancelled event is received.
     * Releases the previously reserved stock back to available.</p>
     */
    @Override
    public void releaseStock(UUID productId, int quantity, UUID orderId) {
        log.info("Releasing reserved stock: productId={}, qty={}, orderId={}",
                productId, quantity, orderId);

        InventoryItem item = persistenceAdapter.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory item not found for product: " + productId));

        // Release the reservation
        item.releaseStock(quantity);
        persistenceAdapter.save(item);

        // Record the transaction
        persistenceAdapter.recordTransaction(productId, "RELEASE", quantity, orderId);

        // Publish release event
        eventPublisher.publishStockReleased(orderId, productId, quantity);

        log.info("Stock released: productId={}, qty={}, orderId={}", productId, quantity, orderId);
    }

    @Override
    public InventoryItem addStock(UUID productId, int quantity) {
        log.info("Adding stock: productId={}, qty={}", productId, quantity);

        InventoryItem item = persistenceAdapter.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Inventory item not found for product: " + productId));

        item.addStock(quantity);
        InventoryItem saved = persistenceAdapter.save(item);

        // Record the transaction
        persistenceAdapter.recordTransaction(productId, "ADD", quantity, null);

        log.info("Stock added: productId={}, newAvailable={}", productId, saved.getQuantityAvailable());
        return saved;
    }
}
