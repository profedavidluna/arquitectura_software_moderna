package com.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order Domain Model
 * 
 * <p>Core domain entity representing a customer order.
 * Contains business rules for order lifecycle management.</p>
 * 
 * <p><b>Saga Pattern</b>: The order status transitions are driven by
 * events received from other services via the ESB. This model
 * encapsulates the state machine logic.</p>
 * 
 * <p><b>SOLID - SRP</b>: This class manages order state and business rules.
 * It doesn't handle persistence, messaging, or presentation.</p>
 */
public class Order {

    private UUID id;
    private UUID userId;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {
        this.id = UUID.randomUUID();
        this.status = OrderStatus.PENDING;
        this.items = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a new order.
     */
    public static Order create(UUID userId, List<OrderItem> items) {
        Order order = new Order();
        order.setUserId(userId);
        order.setItems(items);
        order.calculateTotal();
        return order;
    }

    /**
     * Business rule: Calculate total amount from items.
     */
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Saga step: Confirm order when stock is reserved.
     * <p><b>Saga Pattern</b>: This is a compensatable transaction step.</p>
     */
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm order in status: " + this.status);
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Saga step: Cancel order (compensation action).
     * <p><b>Saga Pattern</b>: This is the compensating transaction that
     * undoes the order creation when stock is insufficient.</p>
     */
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Cannot cancel order in status: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Add an item to the order.
     */
    public void addItem(OrderItem item) {
        this.items.add(item);
        calculateTotal();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
