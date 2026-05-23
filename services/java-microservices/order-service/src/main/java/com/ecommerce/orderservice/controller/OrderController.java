package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates an order from the user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "503", description = "External service unavailable")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/v1/orders - Creating order for user: {}", request.getUserId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details by its UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        log.info("GET /api/v1/orders/{}", id);
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Lists orders with pagination and optional status filter")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public ResponseEntity<PagedResponse<OrderResponse>> listOrders(
            @Parameter(description = "Filter by status") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("GET /api/v1/orders - status={}, userId={}, page={}, size={}", status, userId, page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<OrderResponse> response;
        if (userId != null && status != null) {
            response = orderService.getOrdersByUserAndStatus(userId, status, pageable);
        } else if (userId != null) {
            response = orderService.getOrdersByUser(userId, pageable);
        } else if (status != null) {
            response = orderService.getOrdersByStatus(status, pageable);
        } else {
            response = orderService.listOrders(pageable);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's orders", description = "Retrieves all orders for a specific user")
    @ApiResponse(responseCode = "200", description = "User orders retrieved successfully")
    public ResponseEntity<PagedResponse<OrderResponse>> getUserOrders(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/orders/user/{}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<OrderResponse> response = orderService.getOrdersByUser(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Updates the status of an order with validation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        log.info("PUT /api/v1/orders/{}/status - newStatus={}", id, request.getStatus());
        OrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an order if it's in a cancellable state")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order UUID") @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request) {

        log.info("POST /api/v1/orders/{}/cancel - reason={}", id, request.getReason());
        OrderResponse response = orderService.cancelOrder(id, request.getReason(), request.getCancelledBy());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get order status history", description = "Retrieves the complete status change history for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(
            @Parameter(description = "Order UUID") @PathVariable UUID id) {

        log.info("GET /api/v1/orders/{}/history", id);
        List<OrderStatusHistoryResponse> history = orderService.getOrderHistory(id);
        return ResponseEntity.ok(history);
    }
}
