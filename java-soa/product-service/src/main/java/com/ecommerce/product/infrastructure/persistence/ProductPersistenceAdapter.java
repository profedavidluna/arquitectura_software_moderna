package com.ecommerce.product.infrastructure.persistence;

import com.ecommerce.product.domain.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product Persistence Adapter - Infrastructure Layer
 * 
 * <p><b>Adapter Pattern</b>: This class adapts the Spring Data JPA repository
 * to the domain layer's needs. It translates between domain models and JPA entities.</p>
 * 
 * <p><b>SOLID - DIP</b>: The application layer depends on abstractions (domain model),
 * and this adapter provides the concrete implementation that bridges to JPA.</p>
 * 
 * <p><b>Why an Adapter?</b></p>
 * <ul>
 *   <li>Isolates the domain from JPA/Hibernate specifics</li>
 *   <li>Allows switching persistence technology without affecting business logic</li>
 *   <li>Keeps mapping logic in one place</li>
 * </ul>
 */
@Component
public class ProductPersistenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProductPersistenceAdapter.class);

    private final ProductRepository repository;

    public ProductPersistenceAdapter(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves a domain product by converting it to a JPA entity.
     */
    public Product save(Product product) {
        ProductEntity entity = toEntity(product);
        ProductEntity saved = repository.save(entity);
        log.debug("Product persisted: id={}", saved.getId());
        return toDomain(saved);
    }

    /**
     * Finds a product by ID and converts it back to a domain model.
     */
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    /**
     * Finds all active products.
     */
    public List<Product> findAllActive() {
        return repository.findByActiveTrue()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Finds products by category.
     */
    public List<Product> findByCategory(String category) {
        return repository.findByCategoryAndActiveTrue(category)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Mapping Methods - Convert between Domain and Entity
    // =========================================================================

    /**
     * Maps a domain Product to a JPA ProductEntity.
     */
    private ProductEntity toEntity(Product product) {
        return ProductEntity.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .sku(product.getSku())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Maps a JPA ProductEntity back to a domain Product.
     */
    private Product toDomain(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.getSku(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
