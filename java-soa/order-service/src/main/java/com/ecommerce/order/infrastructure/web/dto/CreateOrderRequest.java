package com.ecommerce.order.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Create Order Request DTO
 * 
 * <p><b>DTO Pattern</b>: Defines the API contract for order creation.
 * Validates input before it reaches the domain layer.</p>
 */
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(UUID userId, List<OrderItemRequest> items) {
        this.userId = userId;
        this.items = items;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
