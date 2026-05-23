package com.ecommerce.paymentservice.integration;

import com.ecommerce.paymentservice.config.TestKafkaConfig;
import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.Transaction;
import com.ecommerce.paymentservice.entity.enums.*;
import com.ecommerce.paymentservice.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests using H2 in-memory database (test profile).
 * For full TestContainers-based tests, see PaymentTestContainersIT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID testUserId;
    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Full payment lifecycle: process -> get -> refund")
    void shouldCompleteFullPaymentLifecycle() throws Exception {
        // Step 1: Process payment
        ProcessPaymentRequest paymentRequest = ProcessPaymentRequest.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.PAYPAL) // PayPal always succeeds in simulation
                .idempotencyKey("test-idem-" + UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .build();

        String paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(150.00))
                .andReturn().getResponse().getContentAsString();

        // Extract transaction ID
        ApiResponse response = objectMapper.readValue(paymentResult, ApiResponse.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) response.getData();
        String transactionId = (String) data.get("id");

        // Step 2: Get payment by ID
        mockMvc.perform(get("/api/v1/payments/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(transactionId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Step 3: Get payments by order
        mockMvc.perform(get("/api/v1/payments/order/{orderId}", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));

        // Step 4: Process partial refund
        RefundRequest refundRequest = RefundRequest.builder()
                .amount(new BigDecimal("50.00"))
                .reason("Customer changed mind")
                .reasonCategory(ReasonCategory.CUSTOMER_REQUEST)
                .build();

        mockMvc.perform(post("/api/v1/payments/{id}/refund", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should save and retrieve payment methods")
    void shouldSaveAndRetrievePaymentMethods() throws Exception {
        // Save payment method
        SavePaymentMethodRequest request = SavePaymentMethodRequest.builder()
                .userId(testUserId)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .token("tok_test_" + UUID.randomUUID())
                .lastFour("4242")
                .cardBrand("VISA")
                .expiryMonth(12)
                .expiryYear(2026)
                .billingName("John Doe")
                .isDefault(true)
                .build();

        mockMvc.perform(post("/api/v1/payments/methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lastFour").value("4242"))
                .andExpect(jsonPath("$.data.cardBrand").value("VISA"));

        // Retrieve payment methods
        mockMvc.perform(get("/api/v1/payments/methods/user/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].lastFour").value("4242"));
    }

    @Test
    @DisplayName("Should enforce idempotency")
    void shouldEnforceIdempotency() throws Exception {
        String idempotencyKey = "unique-key-" + UUID.randomUUID();

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(testOrderId)
                .userId(testUserId)
                .amount(new BigDecimal("75.00"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.PAYPAL)
                .idempotencyKey(idempotencyKey)
                .build();

        // First request should succeed
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second request with same idempotency key should return 409
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should get user payment history")
    void shouldGetUserPaymentHistory() throws Exception {
        // Create a transaction directly in the database
        Transaction transaction = Transaction.builder()
                .transactionNumber("TXN-TEST-001")
                .orderId(testOrderId)
                .userId(testUserId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.STRIPE)
                .gatewayTransactionId("ch_test123")
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        // Get user payment history
        mockMvc.perform(get("/api/v1/payments/user/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("Should return 404 for non-existent payment")
    void shouldReturn404ForNonExistentPayment() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should validate payment request")
    void shouldValidatePaymentRequest() throws Exception {
        // Missing required fields
        ProcessPaymentRequest invalidRequest = ProcessPaymentRequest.builder()
                .amount(new BigDecimal("0.00")) // Invalid: must be > 0
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
