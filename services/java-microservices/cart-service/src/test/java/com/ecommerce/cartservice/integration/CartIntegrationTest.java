package com.ecommerce.cartservice.integration;

import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartStatus;
import com.ecommerce.cartservice.repository.CartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cartRepository.deleteAll();
    }

    @Test
    @DisplayName("Full cart lifecycle: create -> add items -> update -> apply coupon -> clear")
    void fullCartLifecycle() throws Exception {
        // 1. Create cart
        CreateCartRequest createRequest = CreateCartRequest.builder()
                .userId(userId)
                .sessionId("test-session")
                .currency("USD")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.itemCount").value(0))
                .andReturn();

        CartResponse cartResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CartResponse.class);
        UUID cartId = cartResponse.getId();

        // 2. Add first item
        AddItemRequest addItem1 = AddItemRequest.builder()
                .productId(UUID.randomUUID())
                .productName("Laptop")
                .productSku("LAP-001")
                .productImageUrl("https://example.com/laptop.jpg")
                .quantity(1)
                .unitPrice(new BigDecimal("999.99"))
                .build();

        mockMvc.perform(post("/api/v1/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItem1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.subtotal").value(999.99));

        // 3. Add second item
        AddItemRequest addItem2 = AddItemRequest.builder()
                .productId(UUID.randomUUID())
                .productName("Mouse")
                .productSku("MOU-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/v1/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItem2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(3))
                .andReturn();

        CartResponse afterAdd = objectMapper.readValue(
                addResult.getResponse().getContentAsString(), CartResponse.class);
        UUID mouseItemId = afterAdd.getItems().stream()
                .filter(i -> "Mouse".equals(i.getProductName()))
                .findFirst()
                .map(CartItemResponse::getId)
                .orElseThrow();

        // 4. Update mouse quantity
        UpdateItemRequest updateRequest = UpdateItemRequest.builder().quantity(3).build();

        mockMvc.perform(put("/api/v1/carts/{id}/items/{itemId}", cartId, mouseItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(4));

        // 5. Apply coupon
        ApplyCouponRequest couponRequest = ApplyCouponRequest.builder()
                .couponCode("SAVE10PCT")
                .build();

        mockMvc.perform(post("/api/v1/carts/{id}/coupon", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("SAVE10PCT"));

        // 6. Get cart and verify totals
        mockMvc.perform(get("/api/v1/carts/{id}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.couponCode").value("SAVE10PCT"));

        // 7. Remove coupon
        mockMvc.perform(delete("/api/v1/carts/{id}/coupon", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").doesNotExist());

        // 8. Clear cart
        mockMvc.perform(delete("/api/v1/carts/{id}", cartId))
                .andExpect(status().isNoContent());

        // Verify cart is empty
        mockMvc.perform(get("/api/v1/carts/{id}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0))
                .andExpect(jsonPath("$.subtotal").value(0));
    }

    @Test
    @DisplayName("Should get active cart for user")
    void shouldGetActiveCartForUser() throws Exception {
        // Create cart
        CreateCartRequest request = CreateCartRequest.builder()
                .userId(userId)
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get active cart for user
        mockMvc.perform(get("/api/v1/carts/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent cart")
    void shouldReturn404ForNonExistentCart() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/carts/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle adding same product twice - increases quantity")
    void shouldIncreaseQuantityForDuplicateProduct() throws Exception {
        // Create cart
        CreateCartRequest createRequest = CreateCartRequest.builder()
                .userId(userId)
                .currency("USD")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CartResponse cart = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CartResponse.class);
        UUID cartId = cart.getId();
        UUID productId = UUID.randomUUID();

        // Add item first time
        AddItemRequest addItem = AddItemRequest.builder()
                .productId(productId)
                .productName("Widget")
                .productSku("WID-001")
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2));

        // Add same product again
        mockMvc.perform(post("/api/v1/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(4));
    }

    @Test
    @DisplayName("Should calculate free shipping for orders over threshold")
    void shouldCalculateFreeShipping() throws Exception {
        // Create cart
        CreateCartRequest createRequest = CreateCartRequest.builder()
                .userId(userId)
                .currency("USD")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CartResponse cart = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CartResponse.class);
        UUID cartId = cart.getId();

        // Add expensive item (over $50 threshold)
        AddItemRequest addItem = AddItemRequest.builder()
                .productId(UUID.randomUUID())
                .productName("Expensive Item")
                .productSku("EXP-001")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingAmount").value(0));
    }
}
