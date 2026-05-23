package com.ecommerce.paymentservice.integration;

import com.ecommerce.paymentservice.config.TestKafkaConfig;
import com.ecommerce.paymentservice.dto.ProcessPaymentRequest;
import com.ecommerce.paymentservice.dto.SavePaymentMethodRequest;
import com.ecommerce.paymentservice.entity.enums.PaymentGateway;
import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests using TestContainers with PostgreSQL.
 * These tests verify the full stack with a real database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
class PaymentTestContainersIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("payment_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should process payment with PostgreSQL")
    void shouldProcessPaymentWithPostgres() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.PAYPAL)
                .idempotencyKey("tc-idem-" + UUID.randomUUID())
                .ipAddress("10.0.0.1")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.transactionNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.paymentGateway").value("PAYPAL"));
    }

    @Test
    @DisplayName("Should persist and retrieve payment methods with PostgreSQL")
    void shouldPersistPaymentMethodsWithPostgres() throws Exception {
        UUID userId = UUID.randomUUID();

        SavePaymentMethodRequest request = SavePaymentMethodRequest.builder()
                .userId(userId)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .token("tok_tc_" + UUID.randomUUID())
                .lastFour("1234")
                .cardBrand("MASTERCARD")
                .expiryMonth(6)
                .expiryYear(2027)
                .billingName("Jane Smith")
                .isDefault(true)
                .build();

        // Save
        mockMvc.perform(post("/api/v1/payments/methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lastFour").value("1234"))
                .andExpect(jsonPath("$.data.cardBrand").value("MASTERCARD"));

        // Retrieve
        mockMvc.perform(get("/api/v1/payments/methods/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].lastFour").value("1234"));
    }

    @Test
    @DisplayName("Should handle JSONB columns correctly with PostgreSQL")
    void shouldHandleJsonbColumnsCorrectly() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.PAYPAL)
                .paymentGateway(PaymentGateway.PAYPAL)
                .idempotencyKey("tc-jsonb-" + UUID.randomUUID())
                .build();

        // Process payment - gateway response is stored as JSONB
        String result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gatewayTransactionId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Verify we can retrieve it (JSONB was stored and read correctly)
        @SuppressWarnings("unchecked")
        var response = objectMapper.readValue(result, java.util.Map.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) response.get("data");
        String txnId = (String) data.get("id");

        mockMvc.perform(get("/api/v1/payments/{id}", txnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gatewayTransactionId").isNotEmpty());
    }
}
