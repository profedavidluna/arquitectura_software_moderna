package com.ecommerce.order.application;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.infrastructure.messaging.OrderEventPublisher;
import com.ecommerce.order.infrastructure.persistence.OrderPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for OrderServiceImpl
 * 
 * <p>Tests the order saga orchestration logic in isolation.</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderPersistenceAdapter persistenceAdapter;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        OrderItem item = OrderItem.create(
                UUID.randomUUID(),
                "Test Product",
                2,
                new BigDecimal("29.99")
        );
        sampleOrder = Order.create(UUID.randomUUID(), List.of(item));
    }

    @Test
    @DisplayName("Should create order with PENDING status and publish event")
    void createOrder_ShouldSaveAndPublishEvent() {
        // Arrange
        when(persistenceAdapter.save(any(Order.class))).thenReturn(sampleOrder);
        doNothing().when(eventPublisher).publishOrderCreated(any(Order.class));

        // Act
        Order result = orderService.createOrder(sampleOrder);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(persistenceAdapter, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishOrderCreated(any(Order.class));
    }

    @Test
    @DisplayName("Should confirm order and publish confirmed event")
    void confirmOrder_ShouldUpdateStatusAndPublish() {
        // Arrange
        UUID orderId = sampleOrder.getId();
        when(persistenceAdapter.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(persistenceAdapter.save(any(Order.class))).thenReturn(sampleOrder);
        doNothing().when(eventPublisher).publishOrderConfirmed(any(Order.class));

        // Act
        orderService.confirmOrder(orderId);

        // Assert
        assertEquals(OrderStatus.CONFIRMED, sampleOrder.getStatus());
        verify(persistenceAdapter, times(1)).save(any(Order.class));
        verify(eventPublisher, times(1)).publishOrderConfirmed(any(Order.class));
    }

    @Test
    @DisplayName("Should cancel order and publish cancelled event")
    void cancelOrder_ShouldUpdateStatusAndPublish() {
        // Arrange
        UUID orderId = sampleOrder.getId();
        when(persistenceAdapter.findById(orderId)).thenReturn(Optional.of(sampleOrder));
        when(persistenceAdapter.save(any(Order.class))).thenReturn(sampleOrder);
        doNothing().when(eventPublisher).publishOrderCancelled(any(Order.class));

        // Act
        orderService.cancelOrder(orderId);

        // Assert
        assertEquals(OrderStatus.CANCELLED, sampleOrder.getStatus());
        verify(eventPublisher, times(1)).publishOrderCancelled(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when confirming non-existent order")
    void confirmOrder_ShouldThrow_WhenNotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(persistenceAdapter.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.confirmOrder(orderId));
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void createOrder_ShouldCalculateTotal() {
        // Arrange
        OrderItem item1 = OrderItem.create(UUID.randomUUID(), "Product A", 2, new BigDecimal("10.00"));
        OrderItem item2 = OrderItem.create(UUID.randomUUID(), "Product B", 1, new BigDecimal("25.00"));
        Order order = Order.create(UUID.randomUUID(), List.of(item1, item2));

        when(persistenceAdapter.save(any(Order.class))).thenReturn(order);
        doNothing().when(eventPublisher).publishOrderCreated(any(Order.class));

        // Act
        Order result = orderService.createOrder(order);

        // Assert - total should be (2*10) + (1*25) = 45.00
        assertEquals(new BigDecimal("45.00"), result.getTotalAmount());
    }
}
