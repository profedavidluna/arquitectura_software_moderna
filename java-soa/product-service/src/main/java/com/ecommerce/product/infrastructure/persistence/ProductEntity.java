package com.ecommerce.product.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Product JPA Entity - Infrastructure Layer
 * 
 * <p><b>Adapter Pattern</b>: This entity is an infrastructure concern that maps
 * the domain model to the database schema. It is separate from the domain model
 * to maintain clean architecture boundaries.</p>
 * 
 * <p><b>SOLID - SRP</b>: This class is only responsible for ORM mapping.
 * Business logic lives in the domain model, not here.</p>
 * 
 * <p>Why separate Entity from Domain Model?</p>
 * <ul>
 *   <li>Domain model can evolve independently of the database schema</li>
 *   <li>JPA annotations don't pollute the domain layer</li>
 *   <li>Different serialization strategies for API vs persistence</li>
 * </ul>
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 100)
    private String category;

    @Column(unique = true, nullable = false, length = 100)
    private String sku;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
