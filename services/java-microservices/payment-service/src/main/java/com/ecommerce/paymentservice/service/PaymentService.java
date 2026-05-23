package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.*;
import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.PaymentRetryLog;
import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.entity.Transaction;
import com.ecommerce.paymentservice.entity.enums.*;
import com.ecommerce.paymentservice.event.PaymentEventPublisher;
import com.ecommerce.paymentservice.exception.*;
import com.ecommerce.paymentservice.gateway.PaymentGatewayClient;
import com.ecommerce.paymentservice.gateway.PaymentGatewayFactory;
import com.ecommerce.paymentservice.gateway.PaymentGatewayResponse;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.repository.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long[] RETRY_DELAYS_SECONDS = {1, 2, 4}; // Exponential backoff

    private final TransactionRepository transactionRepository;
    private final RefundRepository refundRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRetryLogRepository retryLogRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentEventPublisher eventPublisher;
    private final FraudDetectionService fraudDetectionService;
    private final PaymentMapper paymentMapper;

    private final AtomicLong transactionCounter = new AtomicLong(0);

    @Transactional
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    public TransactionResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment: orderId={}, userId={}, amount={}",
                request.getOrderId(), request.getUserId(), request.getAmount());

        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate payment detected with idempotencyKey={}", request.getIdempotencyKey());
                throw new DuplicatePaymentException(
                        "Payment already processed with idempotency key: " + request.getIdempotencyKey());
            }
        }

        // Fraud detection
        fraudDetectionService.validateTransaction(request);

        // Resolve payment gateway
        PaymentGateway gateway = request.getPaymentGateway() != null
                ? request.getPaymentGateway() : PaymentGateway.STRIPE;

        // Resolve token from payment method if provided
        String token = resolvePaymentToken(request);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionNumber(generateTransactionNumber())
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(TransactionStatus.PROCESSING)
                .paymentMethod(request.getPaymentMethod())
                .paymentGateway(gateway)
                .idempotencyKey(request.getIdempotencyKey())
                .ipAddress(request.getIpAddress())
                .build();

        transaction = transactionRepository.save(transaction);

        // Process through gateway
        PaymentGatewayClient gatewayClient = gatewayFactory.getClient(gateway);
        PaymentGatewayResponse gatewayResponse = gatewayClient.charge(
                token, request.getAmount(), transaction.getCurrency(), request.getIdempotencyKey());

        // Update transaction based on gateway response
        if (gatewayResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setGatewayTransactionId(gatewayResponse.getTransactionId());
            transaction.setGatewayResponse(gatewayResponse.getRawResponse());
            transaction.setProcessedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            // Publish success event
            eventPublisher.publishPaymentProcessed(transaction);
            log.info("Payment completed: transactionId={}, gatewayTxnId={}",
                    transaction.getId(), gatewayResponse.getTransactionId());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(gatewayResponse.getErrorMessage());
            transaction.setGatewayResponse(gatewayResponse.getRawResponse());
            transaction = transactionRepository.save(transaction);

            // Publish failure event
            eventPublisher.publishPaymentFailed(transaction);
            log.warn("Payment failed: transactionId={}, reason={}",
                    transaction.getId(), gatewayResponse.getErrorMessage());
        }

        return paymentMapper.toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getPaymentById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return paymentMapper.toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getPaymentsByOrderId(UUID orderId) {
        return transactionRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserPaymentHistory(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(paymentMapper::toTransactionResponse);
    }

    @Transactional
    public RefundResponse processRefund(UUID transactionId, RefundRequest request) {
        log.info("Processing refund: transactionId={}, amount={}", transactionId, request.getAmount());

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        // Validate refund eligibility
        validateRefundEligibility(transaction, request);

        // Determine refund amount (full refund if not specified)
        BigDecimal refundAmount = request.getAmount() != null
                ? request.getAmount() : transaction.getAmount();

        // Process refund through gateway
        PaymentGatewayClient gatewayClient = gatewayFactory.getClient(transaction.getPaymentGateway());
        PaymentGatewayResponse gatewayResponse = gatewayClient.refund(
                transaction.getGatewayTransactionId(), refundAmount, transaction.getCurrency());

        // Create refund record
        Refund refund = Refund.builder()
                .refundNumber(generateRefundNumber())
                .transaction(transaction)
                .orderId(transaction.getOrderId())
                .amount(refundAmount)
                .currency(transaction.getCurrency())
                .reason(request.getReason())
                .reasonCategory(request.getReasonCategory())
                .status(gatewayResponse.isSuccess() ? RefundStatus.COMPLETED : RefundStatus.FAILED)
                .gatewayRefundId(gatewayResponse.getTransactionId())
                .gatewayResponse(gatewayResponse.getRawResponse())
                .processedAt(gatewayResponse.isSuccess() ? LocalDateTime.now() : null)
                .build();

        refund = refundRepository.save(refund);

        // Update transaction status if fully refunded
        if (gatewayResponse.isSuccess()) {
            BigDecimal totalRefunded = refundRepository.getTotalRefundedForTransaction(transactionId);
            if (totalRefunded.compareTo(transaction.getAmount()) >= 0) {
                transaction.setStatus(TransactionStatus.REFUNDED);
                transactionRepository.save(transaction);
            }
            log.info("Refund completed: refundId={}, amount={}", refund.getId(), refundAmount);
        } else {
            log.warn("Refund failed: transactionId={}, reason={}", transactionId, gatewayResponse.getErrorMessage());
            throw new RefundException("Refund processing failed: " + gatewayResponse.getErrorMessage());
        }

        return paymentMapper.toRefundResponse(refund);
    }

    @Transactional
    public TransactionResponse retryFailedPayment(UUID transactionId) {
        log.info("Retrying failed payment: transactionId={}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (transaction.getStatus() != TransactionStatus.FAILED) {
            throw new PaymentProcessingException(
                    "Only failed transactions can be retried. Current status: " + transaction.getStatus());
        }

        // Check retry count
        Integer maxAttempt = retryLogRepository.findMaxAttemptByTransactionId(transactionId);
        int currentAttempt = (maxAttempt != null ? maxAttempt : 0) + 1;

        if (currentAttempt > MAX_RETRY_ATTEMPTS) {
            throw new PaymentProcessingException(
                    "Maximum retry attempts (" + MAX_RETRY_ATTEMPTS + ") exceeded for transaction: " + transactionId);
        }

        // Process retry
        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        String token = "tok_retry_" + transaction.getId();
        PaymentGatewayClient gatewayClient = gatewayFactory.getClient(transaction.getPaymentGateway());
        PaymentGatewayResponse gatewayResponse = gatewayClient.charge(
                token, transaction.getAmount(), transaction.getCurrency(), null);

        // Log retry attempt
        PaymentRetryLog retryLog = PaymentRetryLog.builder()
                .transaction(transaction)
                .attemptNumber(currentAttempt)
                .status(gatewayResponse.isSuccess() ? RetryStatus.SUCCESS : RetryStatus.FAILED)
                .errorCode(gatewayResponse.getErrorCode())
                .errorMessage(gatewayResponse.getErrorMessage())
                .gatewayResponse(gatewayResponse.getRawResponse())
                .nextRetryAt(gatewayResponse.isSuccess() ? null :
                        LocalDateTime.now().plusSeconds(RETRY_DELAYS_SECONDS[Math.min(currentAttempt - 1, RETRY_DELAYS_SECONDS.length - 1)]))
                .build();
        retryLogRepository.save(retryLog);

        // Update transaction
        if (gatewayResponse.isSuccess()) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setGatewayTransactionId(gatewayResponse.getTransactionId());
            transaction.setGatewayResponse(gatewayResponse.getRawResponse());
            transaction.setProcessedAt(LocalDateTime.now());
            transaction.setFailureReason(null);
            transactionRepository.save(transaction);
            eventPublisher.publishPaymentProcessed(transaction);
            log.info("Retry successful: transactionId={}, attempt={}", transactionId, currentAttempt);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(gatewayResponse.getErrorMessage());
            transaction.setGatewayResponse(gatewayResponse.getRawResponse());
            transactionRepository.save(transaction);
            eventPublisher.publishPaymentFailed(transaction);
            log.warn("Retry failed: transactionId={}, attempt={}, reason={}",
                    transactionId, currentAttempt, gatewayResponse.getErrorMessage());
        }

        return paymentMapper.toTransactionResponse(transaction);
    }

    // Payment Method operations

    @Transactional
    public PaymentMethodResponse savePaymentMethod(SavePaymentMethodRequest request) {
        log.info("Saving payment method for userId={}, type={}", request.getUserId(), request.getMethodType());

        // Check for duplicate token
        if (paymentMethodRepository.existsByUserIdAndTokenAndIsActiveTrue(request.getUserId(), request.getToken())) {
            throw new DuplicatePaymentException("Payment method with this token already exists");
        }

        PaymentMethod paymentMethod = paymentMapper.toPaymentMethodEntity(request);

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(request.getUserId())
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        paymentMethodRepository.save(existing);
                    });
        }

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Payment method saved: id={}, userId={}", paymentMethod.getId(), request.getUserId());
        return paymentMapper.toPaymentMethodResponse(paymentMethod);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getUserPaymentMethods(UUID userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(paymentMapper::toPaymentMethodResponse)
                .toList();
    }

    // Private helpers

    private void validateRefundEligibility(Transaction transaction, RefundRequest request) {
        if (transaction.getStatus() != TransactionStatus.COMPLETED &&
                transaction.getStatus() != TransactionStatus.REFUNDED) {
            throw new RefundException(
                    "Cannot refund transaction with status: " + transaction.getStatus());
        }

        if (transaction.getGatewayTransactionId() == null) {
            throw new RefundException("Cannot refund: no gateway transaction ID found");
        }

        BigDecimal totalRefunded = refundRepository.getTotalRefundedForTransaction(transaction.getId());
        BigDecimal remainingRefundable = transaction.getAmount().subtract(totalRefunded);

        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : transaction.getAmount();

        if (refundAmount.compareTo(remainingRefundable) > 0) {
            throw new RefundException(String.format(
                    "Refund amount (%.2f) exceeds remaining refundable amount (%.2f)",
                    refundAmount, remainingRefundable));
        }
    }

    private String resolvePaymentToken(ProcessPaymentRequest request) {
        if (request.getPaymentMethodId() != null) {
            PaymentMethod method = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", request.getPaymentMethodId()));
            return method.getToken();
        }
        // Generate a simulated token for direct payments
        return "tok_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private String generateTransactionNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = transactionCounter.incrementAndGet();
        return String.format("TXN-%s-%03d", date, seq);
    }

    private String generateRefundNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = transactionCounter.incrementAndGet();
        return String.format("RFN-%s-%03d", date, seq);
    }

    @SuppressWarnings("unused")
    private TransactionResponse processPaymentFallback(ProcessPaymentRequest request, Throwable t) {
        log.error("Payment gateway circuit breaker triggered for orderId={}: {}",
                request.getOrderId(), t.getMessage());
        throw new PaymentProcessingException(
                "Payment service is temporarily unavailable. Please try again later.");
    }
}
