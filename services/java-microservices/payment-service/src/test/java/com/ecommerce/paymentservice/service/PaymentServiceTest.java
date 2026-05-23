package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentRetryLog;
import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.entity.Transaction;
import com.ecommerce.paymentservice.entity.enums.*;
import com.ecommerce.paymentservice.event.PaymentEventPublisher;
import com.ecommerce.paymentservice.exception.*;
import com.ecommerce.paymentservice.gateway.PaymentGatewayFactory;
import com.ecommerce.paymentservice.gateway.PaymentGatewayResponse;
import com.ecommerce.paymentservice.gateway.StripeGatewayClient;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private PaymentRetryLogRepository retryLogRepository;
    @Mock
    private PaymentGatewayFactory gatewayFactory;
    @Mock
    private PaymentEventPublisher eventPublisher;
    @Mock
    private FraudDetectionService fraudDetectionService;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private StripeGatewayClient stripeGatewayClient;

    @InjectMocks
    private PaymentService paymentService;

    private ProcessPaymentRequest validPaymentRequest;
    private Transaction sampleTransaction;
    private TransactionResponse sampleTransactionResponse;

    @BeforeEach
    void setUp() {
        validPaymentRequest = ProcessPaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.STRIPE)
                .idempotencyKey("idem-key-123")
                .ipAddress("192.168.1.1")
                .build();

        sampleTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .transactionNumber("TXN-20261105-001")
                .orderId(validPaymentRequest.getOrderId())
                .userId(validPaymentRequest.getUserId())
                .amount(validPaymentRequest.getAmount())
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.STRIPE)
                .gatewayTransactionId("ch_test123")
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleTransactionResponse = TransactionResponse.builder()
                .id(sampleTransaction.getId())
                .transactionNumber(sampleTransaction.getTransactionNumber())
                .orderId(sampleTransaction.getOrderId())
                .userId(sampleTransaction.getUserId())
                .amount(sampleTransaction.getAmount())
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .paymentMethod(PaymentMethodType.CREDIT_CARD)
                .paymentGateway(PaymentGateway.STRIPE)
                .build();
    }

    @Nested
    @DisplayName("Process Payment Tests")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Should process payment successfully")
        void shouldProcessPaymentSuccessfully() {
            // Given
            when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            doNothing().when(fraudDetectionService).validateTransaction(any());
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
            when(gatewayFactory.getClient(PaymentGateway.STRIPE)).thenReturn(stripeGatewayClient);
            when(stripeGatewayClient.charge(any(), any(), any(), any()))
                    .thenReturn(PaymentGatewayResponse.builder()
                            .success(true)
                            .transactionId("ch_test123")
                            .rawResponse(Map.of("status", "succeeded"))
                            .build());
            when(paymentMapper.toTransactionResponse(any())).thenReturn(sampleTransactionResponse);

            // When
            TransactionResponse result = paymentService.processPayment(validPaymentRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            verify(eventPublisher).publishPaymentProcessed(any());
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw DuplicatePaymentException for duplicate idempotency key")
        void shouldThrowDuplicatePaymentException() {
            // Given
            when(transactionRepository.findByIdempotencyKey("idem-key-123"))
                    .thenReturn(Optional.of(sampleTransaction));

            // When/Then
            assertThatThrownBy(() -> paymentService.processPayment(validPaymentRequest))
                    .isInstanceOf(DuplicatePaymentException.class)
                    .hasMessageContaining("idem-key-123");
        }

        @Test
        @DisplayName("Should handle gateway failure")
        void shouldHandleGatewayFailure() {
            // Given
            when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            doNothing().when(fraudDetectionService).validateTransaction(any());
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
            when(gatewayFactory.getClient(PaymentGateway.STRIPE)).thenReturn(stripeGatewayClient);
            when(stripeGatewayClient.charge(any(), any(), any(), any()))
                    .thenReturn(PaymentGatewayResponse.builder()
                            .success(false)
                            .errorCode("card_declined")
                            .errorMessage("Your card was declined.")
                            .rawResponse(Map.of("error", "card_declined"))
                            .build());

            Transaction failedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .status(TransactionStatus.FAILED)
                    .failureReason("Your card was declined.")
                    .build();
            when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

            TransactionResponse failedResponse = TransactionResponse.builder()
                    .status(TransactionStatus.FAILED)
                    .failureReason("Your card was declined.")
                    .build();
            when(paymentMapper.toTransactionResponse(any())).thenReturn(failedResponse);

            // When
            TransactionResponse result = paymentService.processPayment(validPaymentRequest);

            // Then
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
            verify(eventPublisher).publishPaymentFailed(any());
        }

        @Test
        @DisplayName("Should use saved payment method token when paymentMethodId provided")
        void shouldUseSavedPaymentMethodToken() {
            // Given
            UUID methodId = UUID.randomUUID();
            validPaymentRequest.setPaymentMethodId(methodId);

            PaymentMethod savedMethod = PaymentMethod.builder()
                    .id(methodId)
                    .token("tok_saved_123")
                    .build();

            when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            doNothing().when(fraudDetectionService).validateTransaction(any());
            when(paymentMethodRepository.findById(methodId)).thenReturn(Optional.of(savedMethod));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
            when(gatewayFactory.getClient(PaymentGateway.STRIPE)).thenReturn(stripeGatewayClient);
            when(stripeGatewayClient.charge(eq("tok_saved_123"), any(), any(), any()))
                    .thenReturn(PaymentGatewayResponse.builder()
                            .success(true)
                            .transactionId("ch_test456")
                            .rawResponse(Map.of("status", "succeeded"))
                            .build());
            when(paymentMapper.toTransactionResponse(any())).thenReturn(sampleTransactionResponse);

            // When
            TransactionResponse result = paymentService.processPayment(validPaymentRequest);

            // Then
            assertThat(result).isNotNull();
            verify(stripeGatewayClient).charge(eq("tok_saved_123"), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Get Payment Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should get payment by ID")
        void shouldGetPaymentById() {
            // Given
            UUID id = sampleTransaction.getId();
            when(transactionRepository.findById(id)).thenReturn(Optional.of(sampleTransaction));
            when(paymentMapper.toTransactionResponse(sampleTransaction)).thenReturn(sampleTransactionResponse);

            // When
            TransactionResponse result = paymentService.getPaymentById(id);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent payment")
        void shouldThrowNotFoundForNonExistentPayment() {
            // Given
            UUID id = UUID.randomUUID();
            when(transactionRepository.findById(id)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> paymentService.getPaymentById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get payments by order ID")
        void shouldGetPaymentsByOrderId() {
            // Given
            UUID orderId = UUID.randomUUID();
            when(transactionRepository.findByOrderId(orderId)).thenReturn(List.of(sampleTransaction));
            when(paymentMapper.toTransactionResponse(any())).thenReturn(sampleTransactionResponse);

            // When
            List<TransactionResponse> results = paymentService.getPaymentsByOrderId(orderId);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should get user payment history with pagination")
        void shouldGetUserPaymentHistory() {
            // Given
            UUID userId = UUID.randomUUID();
            PageRequest pageable = PageRequest.of(0, 20);
            Page<Transaction> page = new PageImpl<>(List.of(sampleTransaction));
            when(transactionRepository.findByUserId(userId, pageable)).thenReturn(page);
            when(paymentMapper.toTransactionResponse(any())).thenReturn(sampleTransactionResponse);

            // When
            Page<TransactionResponse> results = paymentService.getUserPaymentHistory(userId, pageable);

            // Then
            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Refund Tests")
    class RefundTests {

        @Test
        @DisplayName("Should process full refund successfully")
        void shouldProcessFullRefund() {
            // Given
            UUID transactionId = sampleTransaction.getId();
            RefundRequest request = RefundRequest.builder()
                    .reason("Customer requested refund")
                    .reasonCategory(ReasonCategory.CUSTOMER_REQUEST)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));
            when(refundRepository.getTotalRefundedForTransaction(transactionId)).thenReturn(BigDecimal.ZERO);
            when(gatewayFactory.getClient(PaymentGateway.STRIPE)).thenReturn(stripeGatewayClient);
            when(stripeGatewayClient.refund(any(), any(), any()))
                    .thenReturn(PaymentGatewayResponse.builder()
                            .success(true)
                            .transactionId("re_test123")
                            .rawResponse(Map.of("status", "succeeded"))
                            .build());

            Refund savedRefund = Refund.builder()
                    .id(UUID.randomUUID())
                    .refundNumber("RFN-20261105-001")
                    .transaction(sampleTransaction)
                    .orderId(sampleTransaction.getOrderId())
                    .amount(sampleTransaction.getAmount())
                    .status(RefundStatus.COMPLETED)
                    .build();
            when(refundRepository.save(any(Refund.class))).thenReturn(savedRefund);

            // After refund, total refunded equals transaction amount
            when(refundRepository.getTotalRefundedForTransaction(transactionId))
                    .thenReturn(BigDecimal.ZERO)
                    .thenReturn(sampleTransaction.getAmount());

            RefundResponse expectedResponse = RefundResponse.builder()
                    .id(savedRefund.getId())
                    .status(RefundStatus.COMPLETED)
                    .build();
            when(paymentMapper.toRefundResponse(any())).thenReturn(expectedResponse);

            // When
            RefundResponse result = paymentService.processRefund(transactionId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should reject refund for non-completed transaction")
        void shouldRejectRefundForNonCompletedTransaction() {
            // Given
            sampleTransaction.setStatus(TransactionStatus.PENDING);
            UUID transactionId = sampleTransaction.getId();
            RefundRequest request = RefundRequest.builder()
                    .reason("Test")
                    .reasonCategory(ReasonCategory.OTHER)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));

            // When/Then
            assertThatThrownBy(() -> paymentService.processRefund(transactionId, request))
                    .isInstanceOf(RefundException.class)
                    .hasMessageContaining("Cannot refund transaction with status");
        }

        @Test
        @DisplayName("Should reject refund exceeding remaining amount")
        void shouldRejectRefundExceedingRemainingAmount() {
            // Given
            UUID transactionId = sampleTransaction.getId();
            RefundRequest request = RefundRequest.builder()
                    .amount(new BigDecimal("200.00"))
                    .reason("Test")
                    .reasonCategory(ReasonCategory.OTHER)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));
            when(refundRepository.getTotalRefundedForTransaction(transactionId)).thenReturn(BigDecimal.ZERO);

            // When/Then
            assertThatThrownBy(() -> paymentService.processRefund(transactionId, request))
                    .isInstanceOf(RefundException.class)
                    .hasMessageContaining("exceeds remaining refundable amount");
        }
    }

    @Nested
    @DisplayName("Retry Payment Tests")
    class RetryPaymentTests {

        @Test
        @DisplayName("Should retry failed payment successfully")
        void shouldRetryFailedPayment() {
            // Given
            sampleTransaction.setStatus(TransactionStatus.FAILED);
            UUID transactionId = sampleTransaction.getId();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));
            when(retryLogRepository.findMaxAttemptByTransactionId(transactionId)).thenReturn(null);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);
            when(gatewayFactory.getClient(PaymentGateway.STRIPE)).thenReturn(stripeGatewayClient);
            when(stripeGatewayClient.charge(any(), any(), any(), any()))
                    .thenReturn(PaymentGatewayResponse.builder()
                            .success(true)
                            .transactionId("ch_retry_123")
                            .rawResponse(Map.of("status", "succeeded"))
                            .build());
            when(retryLogRepository.save(any(PaymentRetryLog.class))).thenReturn(PaymentRetryLog.builder().build());
            when(paymentMapper.toTransactionResponse(any())).thenReturn(sampleTransactionResponse);

            // When
            TransactionResponse result = paymentService.retryFailedPayment(transactionId);

            // Then
            assertThat(result).isNotNull();
            verify(retryLogRepository).save(any(PaymentRetryLog.class));
            verify(eventPublisher).publishPaymentProcessed(any());
        }

        @Test
        @DisplayName("Should reject retry for non-failed transaction")
        void shouldRejectRetryForNonFailedTransaction() {
            // Given
            sampleTransaction.setStatus(TransactionStatus.COMPLETED);
            UUID transactionId = sampleTransaction.getId();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));

            // When/Then
            assertThatThrownBy(() -> paymentService.retryFailedPayment(transactionId))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessageContaining("Only failed transactions can be retried");
        }

        @Test
        @DisplayName("Should reject retry when max attempts exceeded")
        void shouldRejectRetryWhenMaxAttemptsExceeded() {
            // Given
            sampleTransaction.setStatus(TransactionStatus.FAILED);
            UUID transactionId = sampleTransaction.getId();
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(sampleTransaction));
            when(retryLogRepository.findMaxAttemptByTransactionId(transactionId)).thenReturn(3);

            // When/Then
            assertThatThrownBy(() -> paymentService.retryFailedPayment(transactionId))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessageContaining("Maximum retry attempts");
        }
    }

    @Nested
    @DisplayName("Payment Method Tests")
    class PaymentMethodTests {

        @Test
        @DisplayName("Should save payment method successfully")
        void shouldSavePaymentMethod() {
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

            PaymentMethod savedMethod = PaymentMethod.builder()
                    .id(UUID.randomUUID())
                    .userId(request.getUserId())
                    .methodType(PaymentMethodType.CREDIT_CARD)
                    .token("tok_visa_123")
                    .lastFour("4242")
                    .cardBrand("VISA")
                    .isDefault(true)
                    .isActive(true)
                    .build();

            PaymentMethodResponse expectedResponse = PaymentMethodResponse.builder()
                    .id(savedMethod.getId())
                    .userId(request.getUserId())
                    .methodType(PaymentMethodType.CREDIT_CARD)
                    .lastFour("4242")
                    .cardBrand("VISA")
                    .isDefault(true)
                    .build();

            when(paymentMethodRepository.existsByUserIdAndTokenAndIsActiveTrue(request.getUserId(), request.getToken()))
                    .thenReturn(false);
            when(paymentMapper.toPaymentMethodEntity(request)).thenReturn(savedMethod);
            when(paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(request.getUserId()))
                    .thenReturn(Optional.empty());
            when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(savedMethod);
            when(paymentMapper.toPaymentMethodResponse(savedMethod)).thenReturn(expectedResponse);

            // When
            PaymentMethodResponse result = paymentService.savePaymentMethod(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLastFour()).isEqualTo("4242");
            assertThat(result.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("Should throw DuplicatePaymentException for duplicate token")
        void shouldThrowDuplicateForExistingToken() {
            // Given
            SavePaymentMethodRequest request = SavePaymentMethodRequest.builder()
                    .userId(UUID.randomUUID())
                    .methodType(PaymentMethodType.CREDIT_CARD)
                    .token("tok_existing")
                    .build();

            when(paymentMethodRepository.existsByUserIdAndTokenAndIsActiveTrue(request.getUserId(), request.getToken()))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> paymentService.savePaymentMethod(request))
                    .isInstanceOf(DuplicatePaymentException.class);
        }

        @Test
        @DisplayName("Should get user payment methods")
        void shouldGetUserPaymentMethods() {
            // Given
            UUID userId = UUID.randomUUID();
            PaymentMethod method = PaymentMethod.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .methodType(PaymentMethodType.CREDIT_CARD)
                    .lastFour("4242")
                    .isActive(true)
                    .build();

            PaymentMethodResponse response = PaymentMethodResponse.builder()
                    .id(method.getId())
                    .userId(userId)
                    .lastFour("4242")
                    .build();

            when(paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(method));
            when(paymentMapper.toPaymentMethodResponse(method)).thenReturn(response);

            // When
            List<PaymentMethodResponse> results = paymentService.getUserPaymentMethods(userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getLastFour()).isEqualTo("4242");
        }
    }
}
