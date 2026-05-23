package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.entity.CartStatus;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.exception.GlobalExceptionHandler;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private ObjectMapper objectMapper;
    private UUID cartId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        cartId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/carts - Should create cart")
    void shouldCreateCart() throws Exception {
        CreateCartRequest request = CreateCartRequest.builder()
                .userId(userId)
                .sessionId("session-123")
                .currency("USD")
                .build();

        CartResponse response = CartResponse.builder()
                .id(cartId)
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .currency("USD")
                .itemCount(0)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        when(cartService.createCart(any(CreateCartRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/carts - Should return 400 for invalid request")
    void shouldReturn400ForInvalidCreateRequest() throws Exception {
        CreateCartRequest request = CreateCartRequest.builder().build(); // Missing userId

        mockMvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/carts/{id} - Should get cart")
    void shouldGetCart() throws Exception {
        CartResponse response = CartResponse.builder()
                .id(cartId)
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .itemCount(2)
                .subtotal(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("59.99"))
                .items(List.of())
                .build();

        when(cartService.getCart(cartId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/carts/{id}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.itemCount").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/carts/{id} - Should return 404 when not found")
    void shouldReturn404WhenCartNotFound() throws Exception {
        when(cartService.getCart(cartId)).thenThrow(new CartNotFoundException(cartId));

        mockMvc.perform(get("/api/v1/carts/{id}", cartId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/carts/user/{userId} - Should get active cart for user")
    void shouldGetActiveCartForUser() throws Exception {
        CartResponse response = CartResponse.builder()
                .id(cartId)
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .build();

        when(cartService.getActiveCartForUser(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/carts/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/carts/{id}/items - Should add item")
    void shouldAddItem() throws Exception {
        AddItemRequest request = AddItemRequest.builder()
                .productId(UUID.randomUUID())
                .productName("Test Product")
                .productSku("SKU-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .build();

        CartResponse response = CartResponse.builder()
                .id(cartId)
                .itemCount(2)
                .subtotal(new BigDecimal("50.00"))
                .build();

        when(cartService.addItem(eq(cartId), any(AddItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/carts/{id}/items/{itemId} - Should update quantity")
    void shouldUpdateItemQuantity() throws Exception {
        UUID itemId = UUID.randomUUID();
        UpdateItemRequest request = UpdateItemRequest.builder().quantity(5).build();

        CartResponse response = CartResponse.builder()
                .id(cartId)
                .itemCount(5)
                .build();

        when(cartService.updateItemQuantity(eq(cartId), eq(itemId), any(UpdateItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/carts/{id}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(5));
    }

    @Test
    @DisplayName("DELETE /api/v1/carts/{id}/items/{itemId} - Should remove item")
    void shouldRemoveItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        CartResponse response = CartResponse.builder().id(cartId).itemCount(0).build();

        when(cartService.removeItem(cartId, itemId)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/carts/{id}/items/{itemId}", cartId, itemId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/carts/{id} - Should clear cart")
    void shouldClearCart() throws Exception {
        doNothing().when(cartService).clearCart(cartId);

        mockMvc.perform(delete("/api/v1/carts/{id}", cartId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/carts/{id}/coupon - Should apply coupon")
    void shouldApplyCoupon() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .couponCode("SAVE10PCT")
                .build();

        CartResponse response = CartResponse.builder()
                .id(cartId)
                .couponCode("SAVE10PCT")
                .discountAmount(new BigDecimal("10.00"))
                .build();

        when(cartService.applyCoupon(eq(cartId), any(ApplyCouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/carts/{id}/coupon", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("SAVE10PCT"));
    }

    @Test
    @DisplayName("DELETE /api/v1/carts/{id}/coupon - Should remove coupon")
    void shouldRemoveCoupon() throws Exception {
        CartResponse response = CartResponse.builder()
                .id(cartId)
                .couponCode(null)
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(cartService.removeCoupon(cartId)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/carts/{id}/coupon", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(0));
    }
}
