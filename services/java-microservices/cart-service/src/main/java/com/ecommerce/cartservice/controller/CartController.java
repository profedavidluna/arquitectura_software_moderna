package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping Cart Management API")
public class CartController {

    private final CartService cartService;

    @PostMapping
    @Operation(summary = "Create a new cart", description = "Creates a new shopping cart for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cart created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<CartResponse> createCart(@Valid @RequestBody CreateCartRequest request) {
        log.info("POST /api/v1/carts - Creating cart for userId: {}", request.getUserId());
        CartResponse response = cartService.createCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cart by ID", description = "Retrieves a cart by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "410", description = "Cart expired")
    })
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "Cart ID") @PathVariable UUID id) {
        log.info("GET /api/v1/carts/{}", id);
        CartResponse response = cartService.getCart(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get active cart for user", description = "Retrieves the active cart for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active cart found"),
            @ApiResponse(responseCode = "404", description = "No active cart found for user")
    })
    public ResponseEntity<CartResponse> getActiveCartForUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        log.info("GET /api/v1/carts/user/{}", userId);
        CartResponse response = cartService.getActiveCartForUser(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add item to cart", description = "Adds a product item to the specified cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "410", description = "Cart expired")
    })
    public ResponseEntity<CartResponse> addItem(
            @Parameter(description = "Cart ID") @PathVariable UUID id,
            @Valid @RequestBody AddItemRequest request) {
        log.info("POST /api/v1/carts/{}/items - Adding productId: {}", id, request.getProductId());
        CartResponse response = cartService.addItem(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of an item in the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found")
    })
    public ResponseEntity<CartResponse> updateItemQuantity(
            @Parameter(description = "Cart ID") @PathVariable UUID id,
            @Parameter(description = "Cart Item ID") @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        log.info("PUT /api/v1/carts/{}/items/{} - Updating quantity to: {}", id, itemId, request.getQuantity());
        CartResponse response = cartService.updateItemQuantity(id, itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Removes an item from the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found")
    })
    public ResponseEntity<CartResponse> removeItem(
            @Parameter(description = "Cart ID") @PathVariable UUID id,
            @Parameter(description = "Cart Item ID") @PathVariable UUID itemId) {
        log.info("DELETE /api/v1/carts/{}/items/{}", id, itemId);
        CartResponse response = cartService.removeItem(id, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Clear cart", description = "Removes all items from the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "Cart ID") @PathVariable UUID id) {
        log.info("DELETE /api/v1/carts/{}", id);
        cartService.clearCart(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/coupon")
    @Operation(summary = "Apply coupon to cart", description = "Applies a coupon code to the cart for a discount")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coupon code"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<CartResponse> applyCoupon(
            @Parameter(description = "Cart ID") @PathVariable UUID id,
            @Valid @RequestBody ApplyCouponRequest request) {
        log.info("POST /api/v1/carts/{}/coupon - Applying code: {}", id, request.getCouponCode());
        CartResponse response = cartService.applyCoupon(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/coupon")
    @Operation(summary = "Remove coupon from cart", description = "Removes the applied coupon from the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<CartResponse> removeCoupon(
            @Parameter(description = "Cart ID") @PathVariable UUID id) {
        log.info("DELETE /api/v1/carts/{}/coupon", id);
        CartResponse response = cartService.removeCoupon(id);
        return ResponseEntity.ok(response);
    }
}
