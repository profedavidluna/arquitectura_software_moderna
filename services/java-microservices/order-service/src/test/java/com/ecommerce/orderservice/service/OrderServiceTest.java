package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.*;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.*;
import com.ecommerce.orderservice.event.OrderEventPublisher;
import com.ecommerce.orderservice.exception.*;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private OrderResponse sampleOrderResponse;
    private UUID orderId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-20261105-001")
                .userId(userId)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("8.00"))
                .shippingAmount(new BigDecimal("5.99"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("113.99"))
                .currency("USD")
                .shippingAddress(Address.builder()
                        .street("123 Main St")
                        .city("Springfield")
                        .state("IL")
                        .zipCode("62701")
                        .country("US")
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        sampleOrderResponse = OrderResponse.builder()
                .id(orderId)
                .orderNumber("ORD-20261105-001")
                .userId(userId)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("8.00"))
                .shippingAmount(new BigDecimal("5.99"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("113.99"))
                .currency("USD")
                .items(List.of())
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order from cart successfully")
        void shouldCreateOrderFromCart() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId(userId)
                    .shippingAddress(Address.builder()
                            .street("123 Main St")
                            .city("Springfield")
                            .state("IL")
                            .zipCode("62701")
                            .country("US")
                            .build())
                    .paymentMethod("CREDIT_CARD")
                    .build();

            CartResponse cartResponse = CartResponse.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .items(List.of(CartItem.builder()
                            .productId(UUID.randomUUID())
                            .productName("Test Product")
                            .productSku("SKU-001")
                            .quantity(2)
                            .unitPrice(new BigDecimal("50.00"))
                            .subtotal(new BigDecimal("100.00"))
                            .build()))
                    .subtotal(new BigDecimal("100.00"))
                    .taxAmount(new BigDecimal("8.00"))
                    .shippingAmount(new BigDecimal("5.99"))
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("113.99"))
                    .build();

            when(cartServiceClient.getCart(userId)).thenReturn(cartResponse);
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            // When
            OrderResponse result = orderService.createOrder(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(cartServiceClient).getCart(userId);
            verify(orderRepository).save(any(Order.class));
            verify(eventPublisher).publishOrderCreated(any(Order.class));
            verify(cartServiceClient).clearCart(userId);
        }

        @Test
        @DisplayName("Should throw exception when cart is empty")
        void shouldThrowWhenCartIsEmpty() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId(userId)
                    .shippingAddress(Address.builder().build())
                    .build();

            CartResponse emptyCart = CartResponse.builder()
                    .userId(userId)
                    .items(List.of())
                    .build();

            when(cartServiceClient.getCart(userId)).thenReturn(emptyCart);

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("cart is empty");
        }

        @Test
        @DisplayName("Should throw exception when cart is null")
        void shouldThrowWhenCartIsNull() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId(userId)
                    .shippingAddress(Address.builder().build())
                    .build();

            when(cartServiceClient.getCart(userId)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(InvalidOrderStateException.class);
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order by ID")
        void shouldGetOrderById() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
            when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleOrderResponse);

            OrderResponse result = orderService.getOrderById(orderId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should throw when order not found")
        void shouldThrowWhenOrderNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(orderRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(nonExistentId))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Orders Tests")
    class ListOrdersTests {

        @Test
        @DisplayName("Should list orders with pagination")
        void shouldListOrdersWithPagination() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Order> page = new PageImpl<>(List.of(sampleOrder), pageable, 1);

            when(orderRepository.findAll(pageable)).thenReturn(page);
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            PagedResponse<OrderResponse> result = orderService.listOrders(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should get orders by user")
        void shouldGetOrdersByUser() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Order> page = new PageImpl<>(List.of(sampleOrder), pageable, 1);

            when(orderRepository.findByUserId(userId, pageable)).thenReturn(page);
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            PagedResponse<OrderResponse> result = orderService.getOrdersByUser(userId, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Should get orders by status")
        void shouldGetOrdersByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Order> page = new PageImpl<>(List.of(sampleOrder), pageable, 1);

            when(orderRepository.findByStatus(OrderStatus.PENDING, pageable)).thenReturn(page);
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            PagedResponse<OrderResponse> result = orderService.getOrdersByStatus(OrderStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByStatus(OrderStatus.PENDING, pageable);
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update status from PENDING to CONFIRMED")
        void shouldUpdateStatusPendingToConfirmed() {
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.CONFIRMED)
                    .reason("Payment received")
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            OrderResponse result = orderService.updateOrderStatus(orderId, request);

            assertThat(result).isNotNull();
            verify(orderRepository).save(any(Order.class));
            verify(eventPublisher).publishOrderConfirmed(any(Order.class));
        }

        @Test
        @DisplayName("Should reject invalid status transition")
        void shouldRejectInvalidTransition() {
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.PENDING)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(InvalidOrderStateException.class);
        }

        @Test
        @DisplayName("Should set shipped timestamp when status is SHIPPED")
        void shouldSetShippedTimestamp() {
            sampleOrder.setStatus(OrderStatus.PROCESSING);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            orderService.updateOrderStatus(orderId, request);

            verify(orderRepository).save(argThat(order -> order.getShippedAt() != null));
            verify(eventPublisher).publishOrderShipped(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel PENDING order")
        void shouldCancelPendingOrder() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            OrderResponse result = orderService.cancelOrder(orderId, "Changed my mind", userId);

            assertThat(result).isNotNull();
            verify(orderRepository).save(argThat(order ->
                    order.getStatus() == OrderStatus.CANCELLED &&
                    order.getCancelledReason().equals("Changed my mind") &&
                    order.getCancelledAt() != null));
            verify(eventPublisher).publishOrderCancelled(any(Order.class));
        }

        @Test
        @DisplayName("Should cancel CONFIRMED order")
        void shouldCancelConfirmedOrder() {
            sampleOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleOrderResponse);

            OrderResponse result = orderService.cancelOrder(orderId, "Out of stock", null);

            assertThat(result).isNotNull();
            verify(eventPublisher).publishOrderCancelled(any(Order.class));
        }

        @Test
        @DisplayName("Should reject cancellation of SHIPPED order")
        void shouldRejectCancellationOfShippedOrder() {
            sampleOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(orderId, "Too late", userId))
                    .isInstanceOf(OrderCancellationException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        @DisplayName("Should reject cancellation of PROCESSING order")
        void shouldRejectCancellationOfProcessingOrder() {
            sampleOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(orderId, "Changed mind", userId))
                    .isInstanceOf(OrderCancellationException.class);
        }
    }

    @Nested
    @DisplayName("Order History Tests")
    class OrderHistoryTests {

        @Test
        @DisplayName("Should get order status history")
        void shouldGetOrderHistory() {
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .id(UUID.randomUUID())
                    .order(sampleOrder)
                    .oldStatus(null)
                    .newStatus(OrderStatus.PENDING)
                    .reason("Order created")
                    .createdAt(LocalDateTime.now())
                    .build();

            OrderStatusHistoryResponse historyResponse = OrderStatusHistoryResponse.builder()
                    .id(history.getId())
                    .oldStatus(null)
                    .newStatus(OrderStatus.PENDING)
                    .reason("Order created")
                    .createdAt(history.getCreatedAt())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(sampleOrder));
            when(statusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId))
                    .thenReturn(List.of(history));
            when(orderMapper.toHistoryResponseList(anyList())).thenReturn(List.of(historyResponse));

            List<OrderStatusHistoryResponse> result = orderService.getOrderHistory(orderId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNewStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }
}
