package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.*;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.*;
import com.ecommerce.orderservice.event.OrderEventPublisher;
import com.ecommerce.orderservice.exception.*;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderMapper orderMapper;
    private final CartServiceClient cartServiceClient;
    private final OrderEventPublisher eventPublisher;

    private static final AtomicLong orderSequence = new AtomicLong(1);

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.REFUNDED, Set.of()
    );

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED
    );

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Fetch cart from Cart Service
        CartResponse cart = cartServiceClient.getCart(request.getUserId());
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new InvalidOrderStateException("Cannot create order: cart is empty");
        }

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Build order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .subtotal(cart.getSubtotal())
                .taxAmount(cart.getTaxAmount() != null ? cart.getTaxAmount() : BigDecimal.ZERO)
                .shippingAmount(cart.getShippingAmount() != null ? cart.getShippingAmount() : BigDecimal.ZERO)
                .discountAmount(cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO)
                .totalAmount(cart.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .notes(request.getNotes())
                .build();

        // Add order items from cart
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .productSku(cartItem.getProductSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .subtotal(cartItem.getSubtotal())
                    .build();
            order.addItem(orderItem);
        }

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Record initial status
        recordStatusChange(savedOrder, null, OrderStatus.PENDING, null, "Order created");

        // Publish order created event (saga starts)
        eventPublisher.publishOrderCreated(savedOrder);

        // Clear cart after successful order creation
        try {
            cartServiceClient.clearCart(request.getUserId());
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {}: {}", request.getUserId(), e.getMessage());
        }

        log.info("Order created successfully: {}", savedOrder.getOrderNumber());
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByUser(UUID userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserId(userId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findByStatus(status, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByUserAndStatus(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return toPagedResponse(page);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(orderId);
        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Set timestamp fields based on status
        switch (newStatus) {
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> order.setCancelledAt(LocalDateTime.now());
            default -> { /* no timestamp update needed */ }
        }

        Order savedOrder = orderRepository.save(order);
        recordStatusChange(savedOrder, oldStatus, newStatus, request.getChangedBy(), request.getReason());

        // Publish events based on new status
        switch (newStatus) {
            case CONFIRMED -> eventPublisher.publishOrderConfirmed(savedOrder);
            case SHIPPED -> eventPublisher.publishOrderShipped(savedOrder);
            case CANCELLED -> eventPublisher.publishOrderCancelled(savedOrder);
            default -> { /* no event for other transitions */ }
        }

        log.info("Order {} status updated: {} -> {}", order.getOrderNumber(), oldStatus, newStatus);
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason, UUID cancelledBy) {
        Order order = findOrderOrThrow(orderId);

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new OrderCancellationException(
                    String.format("Order %s cannot be cancelled. Current status: %s. Only PENDING and CONFIRMED orders can be cancelled.",
                            order.getOrderNumber(), order.getStatus()));
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        recordStatusChange(savedOrder, oldStatus, OrderStatus.CANCELLED, cancelledBy, reason);

        // Publish cancellation event
        eventPublisher.publishOrderCancelled(savedOrder);

        log.info("Order {} cancelled. Reason: {}", order.getOrderNumber(), reason);
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(UUID orderId) {
        findOrderOrThrow(orderId);
        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return orderMapper.toHistoryResponseList(history);
    }

    // --- Private helpers ---

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        Set<OrderStatus> validTargets = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!validTargets.contains(target)) {
            throw new InvalidOrderStateException(current, target);
        }
    }

    private void recordStatusChange(Order order, OrderStatus oldStatus, OrderStatus newStatus, UUID changedBy, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        statusHistoryRepository.save(history);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = orderSequence.getAndIncrement();
        return String.format("ORD-%s-%03d", datePart, seq);
    }

    private PagedResponse<OrderResponse> toPagedResponse(Page<Order> page) {
        List<OrderResponse> content = page.getContent().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
