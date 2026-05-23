package com.ecommerce.order.application;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.service.OrderService;
import com.ecommerce.order.infrastructure.messaging.OrderEventPublisher;
import com.ecommerce.order.infrastructure.persistence.OrderPersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Service Implementation - Application Layer
 * 
 * <p><b>Saga Pattern - Orchestrator</b>: This service acts as the saga orchestrator
 * for the order creation flow. It initiates the saga by publishing events and
 * handles responses from other services.</p>
 * 
 * <p><b>Flow</b>:</p>
 * <ol>
 *   <li>createOrder() → saves order as PENDING → publishes order.created</li>
 *   <li>confirmOrder() → updates to CONFIRMED → publishes order.confirmed</li>
 *   <li>cancelOrder() → updates to CANCELLED → publishes order.cancelled</li>
 * </ol>
 * 
 * <p><b>Strategy Pattern</b>: The order processing strategy could be swapped
 * (e.g., synchronous vs. asynchronous confirmation) by implementing a different
 * version of this service.</p>
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderPersistenceAdapter persistenceAdapter;
    private final OrderEventPublisher eventPublisher;

    public OrderServiceImpl(OrderPersistenceAdapter persistenceAdapter,
                            OrderEventPublisher eventPublisher) {
        this.persistenceAdapter = persistenceAdapter;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new order and initiates the Saga.
     * 
     * <p><b>Saga Step 1</b>: Order is created with PENDING status.
     * The OrderCreatedEvent is published to Kafka, which triggers
     * the Inventory Service to check and reserve stock.</p>
     */
    @Override
    public Order createOrder(Order order) {
        log.info("Creating new order: userId={}, items={}", 
                order.getUserId(), order.getItems().size());

        // Save order with PENDING status
        Order savedOrder = persistenceAdapter.save(order);

        // Publish event to ESB - initiates the saga
        eventPublisher.publishOrderCreated(savedOrder);

        log.info("Order created and saga initiated: orderId={}, status={}",
                savedOrder.getId(), savedOrder.getStatus());
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(UUID id) {
        log.debug("Fetching order by id: {}", id);
        return persistenceAdapter.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(UUID userId) {
        log.debug("Fetching orders for user: {}", userId);
        return persistenceAdapter.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return persistenceAdapter.findAll();
    }

    /**
     * Confirms an order after stock reservation.
     * 
     * <p><b>Saga Step 3</b>: Called when the Inventory Service confirms
     * stock reservation (stock.reserved event received).</p>
     */
    @Override
    public void confirmOrder(UUID orderId) {
        log.info("Confirming order: orderId={}", orderId);

        Order order = persistenceAdapter.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Domain business rule - validates state transition
        order.confirm();

        persistenceAdapter.save(order);

        // Publish confirmation event
        eventPublisher.publishOrderConfirmed(order);

        log.info("Order confirmed: orderId={}", orderId);
    }

    /**
     * Cancels an order (compensating transaction).
     * 
     * <p><b>Saga Compensation</b>: Called when stock is insufficient
     * or when the user explicitly cancels. Publishes order.cancelled
     * so the Inventory Service can release any reserved stock.</p>
     */
    @Override
    public void cancelOrder(UUID orderId) {
        log.info("Cancelling order: orderId={}", orderId);

        Order order = persistenceAdapter.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Domain business rule - validates state transition
        order.cancel();

        persistenceAdapter.save(order);

        // Publish cancellation event - triggers stock release
        eventPublisher.publishOrderCancelled(order);

        log.info("Order cancelled: orderId={}", orderId);
    }
}
