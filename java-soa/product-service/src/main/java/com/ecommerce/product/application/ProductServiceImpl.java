package com.ecommerce.product.application;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.service.ProductService;
import com.ecommerce.product.infrastructure.messaging.ProductEventPublisher;
import com.ecommerce.product.infrastructure.persistence.ProductPersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Product Service Implementation - Application Layer
 * 
 * <p><b>SOLID - SRP</b>: This class orchestrates the product use cases.
 * It coordinates between the persistence layer and the messaging layer,
 * but delegates actual work to each.</p>
 * 
 * <p><b>SOLID - OCP</b>: New event types can be added by creating new
 * publisher methods without modifying existing logic.</p>
 * 
 * <p><b>SOLID - LSP</b>: This implementation is fully substitutable for
 * the ProductService interface. Any consumer using the interface will
 * work correctly with this implementation.</p>
 * 
 * <p><b>Service Layer Pattern</b>: This class acts as the service layer,
 * encapsulating business logic and transaction management.</p>
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductPersistenceAdapter persistenceAdapter;
    private final ProductEventPublisher eventPublisher;

    /**
     * Constructor injection - promotes testability and immutability.
     * <p><b>SOLID - DIP</b>: Dependencies are injected, not created internally.</p>
     */
    public ProductServiceImpl(ProductPersistenceAdapter persistenceAdapter,
                              ProductEventPublisher eventPublisher) {
        this.persistenceAdapter = persistenceAdapter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Product createProduct(Product product) {
        log.info("Creating new product: name={}, sku={}", product.getName(), product.getSku());

        // Persist the product
        Product savedProduct = persistenceAdapter.save(product);

        // Publish event to ESB (Kafka) - Observer Pattern
        // Other services (e.g., Inventory) can react to this event
        eventPublisher.publishProductCreated(savedProduct);

        log.info("Product created successfully: id={}", savedProduct.getId());
        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(UUID id) {
        log.debug("Fetching product by id: {}", id);
        return persistenceAdapter.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.debug("Fetching all active products");
        return persistenceAdapter.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        log.debug("Fetching products by category: {}", category);
        return persistenceAdapter.findByCategory(category);
    }

    @Override
    public Product updateProduct(UUID id, Product product) {
        log.info("Updating product: id={}", id);

        Product existing = persistenceAdapter.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        // Update fields
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setCategory(product.getCategory());
        existing.setUpdatedAt(java.time.LocalDateTime.now());

        Product updated = persistenceAdapter.save(existing);
        log.info("Product updated successfully: id={}", updated.getId());
        return updated;
    }

    @Override
    public void deactivateProduct(UUID id) {
        log.info("Deactivating product: id={}", id);

        Product product = persistenceAdapter.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        product.deactivate();
        persistenceAdapter.save(product);

        log.info("Product deactivated: id={}", id);
    }
}
