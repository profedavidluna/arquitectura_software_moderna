package com.ecommerce.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Order Repository - Spring Data JPA
 * 
 * <p><b>Repository Pattern</b>: Provides collection-like access to order data.
 * Spring Data generates the implementation at runtime.</p>
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /**
     * Find all orders for a specific user.
     */
    List<OrderEntity> findByUserId(UUID userId);

    /**
     * Find orders by status.
     */
    List<OrderEntity> findByStatus(String status);
}
