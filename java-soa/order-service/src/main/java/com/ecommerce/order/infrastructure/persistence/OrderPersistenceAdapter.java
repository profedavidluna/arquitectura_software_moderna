package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Persistence Adapter - Infrastructure Layer
 * 
 * <p><b>Adapter Pattern</b>: Translates between the Order domain model
 * and the JPA entity model. Handles the complexity of mapping
 * one-to-many relationships (Order → OrderItems).</p>
 */
@Component
public class OrderPersistenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(OrderPersistenceAdapter.class);

    private final OrderRepository repository;

    public OrderPersistenceAdapter(OrderRepository repository) {
        this.repository = repository;
    }

    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = repository.save(entity);
        log.debug("Order persisted: id={}, status={}", saved.getId(), saved.getStatus());
        return toDomain(saved);
    }

    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    public List<Order> findByUserId(UUID userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    public List<Order> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Mapping Methods
    // =========================================================================

    private OrderEntity toEntity(Order order) {
        OrderEntity entity = OrderEntity.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(new java.util.ArrayList<>())
                .build();

        // Map order items maintaining bidirectional relationship
        if (order.getItems() != null) {
            order.getItems().forEach(item -> {
                OrderItemEntity itemEntity = OrderItemEntity.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build();
                entity.addItem(itemEntity);
            });
        }

        return entity;
    }

    private Order toDomain(OrderEntity entity) {
        Order order = new Order();
        order.setId(entity.getId());
        order.setUserId(entity.getUserId());
        order.setStatus(OrderStatus.valueOf(entity.getStatus()));
        order.setTotalAmount(entity.getTotalAmount());
        order.setCreatedAt(entity.getCreatedAt());
        order.setUpdatedAt(entity.getUpdatedAt());

        // Map items
        List<OrderItem> items = entity.getItems().stream()
                .map(itemEntity -> new OrderItem(
                        itemEntity.getId(),
                        itemEntity.getProductId(),
                        itemEntity.getProductName(),
                        itemEntity.getQuantity(),
                        itemEntity.getUnitPrice()
                ))
                .collect(Collectors.toList());
        order.setItems(items);

        return order;
    }
}
