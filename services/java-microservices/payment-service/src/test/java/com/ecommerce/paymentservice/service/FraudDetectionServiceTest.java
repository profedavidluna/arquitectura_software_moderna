package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.ProcessPaymentRequest;
import com.ecommerce.paymentservice.entity.Transaction;
import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import com.ecommerce.paymentservice.exception.FraudDetectionException;
import com.ecommerce.paymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Test
    @DisplayName("Should pass fraud detection for valid transaction")
    void shouldPassForValidTransaction() {
        // Given
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .build();

        when(transactionRepository.findRecentByUserId(eq(request.getUserId()), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertDoesNotThrow(() -> fraudDetectionService.validateTransaction(request));
    }

    @Test
    @DisplayName("Should reject transaction exceeding single transaction limit")
    void shouldRejectHighAmountTransaction() {
        // Given
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("15000.00"))
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .build();

        // When/Then
        assertThatThrownBy(() -> fraudDetectionService.validateTransaction(request))
                .isInstanceOf(FraudDetectionException.class)
                .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    @DisplayName("Should reject when transaction velocity exceeded")
    void shouldRejectHighVelocityTransactions() {
        // Given
        UUID userId = UUID.randomUUID();
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .userId(userId)
                .amount(new BigDecimal("50.00"))
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .build();

        // Create 10 recent transactions within the last hour
        List<Transaction> recentTransactions = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Transaction t = Transaction.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .amount(new BigDecimal("10.00"))
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            recentTransactions.add(t);
        }

        when(transactionRepository.findRecentByUserId(eq(userId), any(PageRequest.class)))
                .thenReturn(recentTransactions);

        // When/Then
        assertThatThrownBy(() -> fraudDetectionService.validateTransaction(request))
                .isInstanceOf(FraudDetectionException.class)
                .hasMessageContaining("Too many transactions");
    }
}
