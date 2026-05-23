package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.enums.*;
import com.ecommerce.paymentservice.exception.DuplicatePaymentException;
import com.ecommerce.paymentservice.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /api/v1/payments - Should process payment successfully")
    void shouldProcessPayment() throws Exception {
        // Given
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .transactionNumber("TXN-20261105-001")
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.STRIPE)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any())).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionNumber").value("TXN-20261105-001"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - missing required fields
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .amount(new BigDecimal("-1.00"))
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments - Should return 409 for duplicate payment")
    void shouldReturn409ForDuplicatePayment() throws Exception {
        // Given
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .idempotencyKey("dup-key")
                .build();

        when(paymentService.processPayment(any()))
                .thenThrow(new DuplicatePaymentException("Payment already processed"));

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - Should get payment by ID")
    void shouldGetPaymentById() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        TransactionResponse response = TransactionResponse.builder()
                .id(id)
                .transactionNumber("TXN-20261105-001")
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("99.99"))
                .build();

        when(paymentService.getPaymentById(id)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - Should return 404 for non-existent payment")
    void shouldReturn404ForNonExistentPayment() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        when(paymentService.getPaymentById(id))
                .thenThrow(new ResourceNotFoundException("Transaction", "id", id));

        // When/Then
        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId} - Should get payments for order")
    void shouldGetPaymentsForOrder() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        List<TransactionResponse> responses = List.of(
                TransactionResponse.builder()
                        .id(UUID.randomUUID())
                        .orderId(orderId)
                        .status(TransactionStatus.COMPLETED)
                        .build()
        );

        when(paymentService.getPaymentsByOrderId(orderId)).thenReturn(responses);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{id}/refund - Should process refund")
    void shouldProcessRefund() throws Exception {
        // Given
        UUID transactionId = UUID.randomUUID();
        RefundRequest request = RefundRequest.builder()
                .amount(new BigDecimal("50.00"))
                .reason("Customer requested refund")
                .reasonCategory(ReasonCategory.CUSTOMER_REQUEST)
                .build();

        RefundResponse response = RefundResponse.builder()
                .id(UUID.randomUUID())
                .refundNumber("RFN-20261105-001")
                .transactionId(transactionId)
                .amount(new BigDecimal("50.00"))
                .status(RefundStatus.COMPLETED)
                .build();

        when(paymentService.processRefund(eq(transactionId), any())).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/v1/payments/{id}/refund", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/user/{userId} - Should get user payment history")
    void shouldGetUserPaymentHistory() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Page<TransactionResponse> page = new PageImpl<>(List.of(
                TransactionResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .status(TransactionStatus.COMPLETED)
                        .build()
        ));

        when(paymentService.getUserPaymentHistory(eq(userId), any())).thenReturn(page);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/payments/methods - Should save payment method")
    void shouldSavePaymentMethod() throws Exception {
        // Given
        SavePaymentMethodRequest request = SavePaymentMethodRequest.builder()
                .userId(UUID.randomUUID())
                .methodType(PaymentMethodType.CREDIT_CARD)
                .token("tok_visa_123")
                .lastFour("4242")
                .cardBrand("VISA")
                .expiryMonth(12)
                .expiryYear(2026)
                .isDefault(true)
                .build();

        PaymentMethodResponse response = PaymentMethodResponse.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .methodType(PaymentMethodType.CREDIT_CARD)
                .lastFour("4242")
                .cardBrand("VISA")
                .isDefault(true)
                .isActive(true)
                .build();

        when(paymentService.savePaymentMethod(any())).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/api/v1/payments/methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lastFour").value("4242"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/methods/user/{userId} - Should get user payment methods")
    void shouldGetUserPaymentMethods() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        List<PaymentMethodResponse> responses = List.of(
                PaymentMethodResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .methodType(PaymentMethodType.CREDIT_CARD)
                        .lastFour("4242")
                        .isActive(true)
                        .build()
        );

        when(paymentService.getUserPaymentMethods(userId)).thenReturn(responses);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/methods/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].lastFour").value("4242"));
    }
}
