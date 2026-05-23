package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Address;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.InvalidOrderStateException;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private UUID orderId;
    private UUID userId;
    private OrderResponse sampleResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleResponse = OrderResponse.builder()
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrderEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Should create order successfully")
        void shouldCreateOrder() throws Exception {
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

            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(orderId.toString()))
                    .andExpect(jsonPath("$.orderNumber").value("ORD-20261105-001"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId(null)
                    .shippingAddress(null)
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId(userId)
                    .shippingAddress(Address.builder().build())
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Should get order by ID")
        void shouldGetOrderById() throws Exception {
            when(orderService.getOrderById(orderId)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderId.toString()))
                    .andExpect(jsonPath("$.orderNumber").value("ORD-20261105-001"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 for non-existent order")
        void shouldReturn404ForNonExistentOrder() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(orderService.getOrderById(nonExistentId))
                    .thenThrow(new OrderNotFoundException(nonExistentId));

            mockMvc.perform(get("/api/v1/orders/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class ListOrdersEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Should list orders with pagination")
        void shouldListOrders() throws Exception {
            PagedResponse<OrderResponse> pagedResponse = PagedResponse.<OrderResponse>builder()
                    .content(List.of(sampleResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(orderService.listOrders(any())).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}/status")
    class UpdateStatusEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Should update order status")
        void shouldUpdateOrderStatus() throws Exception {
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.CONFIRMED)
                    .reason("Payment received")
                    .build();

            sampleResponse.setStatus(OrderStatus.CONFIRMED);
            when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/orders/{id}/status", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 409 for invalid transition")
        void shouldReturn409ForInvalidTransition() throws Exception {
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.DELIVERED)
                    .build();

            when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                    .thenThrow(new InvalidOrderStateException(OrderStatus.PENDING, OrderStatus.DELIVERED));

            mockMvc.perform(put("/api/v1/orders/{id}/status", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders/{id}/cancel")
    class CancelOrderEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Should cancel order successfully")
        void shouldCancelOrder() throws Exception {
            CancelOrderRequest request = CancelOrderRequest.builder()
                    .reason("Changed my mind")
                    .cancelledBy(userId)
                    .build();

            sampleResponse.setStatus(OrderStatus.CANCELLED);
            when(orderService.cancelOrder(eq(orderId), eq("Changed my mind"), eq(userId)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when order cannot be cancelled")
        void shouldReturn400WhenCannotCancel() throws Exception {
            CancelOrderRequest request = CancelOrderRequest.builder()
                    .reason("Too late")
                    .build();

            when(orderService.cancelOrder(eq(orderId), eq("Too late"), any()))
                    .thenThrow(new OrderCancellationException("Order cannot be cancelled"));

            mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}/history")
    class OrderHistoryEndpoint {

        @Test
        @WithMockUser
        @DisplayName("Should get order history")
        void shouldGetOrderHistory() throws Exception {
            OrderStatusHistoryResponse historyResponse = OrderStatusHistoryResponse.builder()
                    .id(UUID.randomUUID())
                    .oldStatus(null)
                    .newStatus(OrderStatus.PENDING)
                    .reason("Order created")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(orderService.getOrderHistory(orderId)).thenReturn(List.of(historyResponse));

            mockMvc.perform(get("/api/v1/orders/{id}/history", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].newStatus").value("PENDING"));
        }
    }
}
