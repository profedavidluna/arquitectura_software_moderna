package com.ecommerce.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", unique = true, nullable = false)
    private UUID productId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "reorder_quantity", nullable = false)
    private Integer reorderQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        if (quantityAvailable == null) quantityAvailable = 0;
        if (quantityReserved == null) quantityReserved = 0;
        if (reorderPoint == null) reorderPoint = 10;
        if (reorderQuantity == null) reorderQuantity = 50;
    }
}
