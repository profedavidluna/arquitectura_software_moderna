package com.ecommerce.order.domain.service;

import com.ecommerce.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Service Interface - Domain Layer
 * 
 * <p><b>SOLID - ISP</b>: This interface defines only order management operations.
 * Saga-related operations (confirm, cancel) are included because they are
 * core to the order lifecycle.</p>
 * 
 * <p><b>Strategy Pattern</b>: Different implementations could provide
 * different order processing strategies (e.g., immediate fulfillment vs.
 * batch processing) while maintaining the same interface.</p>
 */
public interface OrderService {

    /**
     * Creates a new order and initiates the order saga.
     * Publishes OrderCreatedEvent to the ESB.
     */
    Order createOrder(Order order);

    /**
     * Retrieves an order by its ID.
     */
    Optional<Order> getOrderById(UUID id);

    /**
     * Retrieves all orders for a specific user.
     */
    List<Order> getOrdersByUserId(UUID userId);

    /**
     * Retrieves all orders (admin operation).
     */
    List<Order> getAllOrders();

    /**
     * Confirms an order (called when stock is reserved).
     * <p><b>Saga Pattern</b>: This is triggered by the stock.reserved event.</p>
     */
    void confirmOrder(UUID orderId);

    /**
     * Cancels an order (called when stock is insufficient or user cancels).
     * <p><b>Saga Pattern</b>: This is the compensating action.</p>
     */
    void cancelOrder(UUID orderId);
}
