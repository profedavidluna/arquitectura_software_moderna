package com.ecommerce.order.infrastructure.web;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderItem;
import com.ecommerce.order.domain.service.OrderService;
import com.ecommerce.order.infrastructure.web.dto.CreateOrderRequest;
import com.ecommerce.order.infrastructure.web.dto.OrderResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order REST Controller - Infrastructure Layer (Web Adapter)
 * 
 * <p><b>SOA - Service Endpoint</b>: Exposes order management capabilities
 * via REST. This is the entry point for the order saga.</p>
 * 
 * <p><b>Adapter Pattern</b>: Translates HTTP requests into domain operations
 * and domain results back into HTTP responses.</p>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders - Create a new order (initiates saga)
     * 
     * <p>This endpoint creates an order with PENDING status and publishes
     * an OrderCreatedEvent to Kafka. The order will be confirmed or cancelled
     * asynchronously based on inventory availability.</p>
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("REST: Create order request: userId={}, items={}",
                request.getUserId(), request.getItems().size());

        // Map DTO to domain model
        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> OrderItem.create(
                        itemReq.getProductId(),
                        itemReq.getProductName(),
                        itemReq.getQuantity(),
                        itemReq.getUnitPrice()
                ))
                .collect(Collectors.toList());

        Order order = Order.create(request.getUserId(), items);
        Order created = orderService.createOrder(order);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.fromDomain(created));
    }

    /**
     * GET /api/orders/{id} - Get order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        return orderService.getOrderById(id)
                .map(order -> ResponseEntity.ok(OrderResponse.fromDomain(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/orders - Get all orders (optionally filtered by user)
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) UUID userId) {

        List<Order> orders;
        if (userId != null) {
            orders = orderService.getOrdersByUserId(userId);
        } else {
            orders = orderService.getAllOrders();
        }

        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/orders/{id}/cancel - Cancel an order
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        log.info("REST: Cancel order request: orderId={}", id);
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
}
