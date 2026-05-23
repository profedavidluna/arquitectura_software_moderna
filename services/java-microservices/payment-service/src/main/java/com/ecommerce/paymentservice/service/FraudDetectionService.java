package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.ProcessPaymentRequest;
import com.ecommerce.paymentservice.exception.FraudDetectionException;
import com.ecommerce.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Basic fraud detection service.
 * Checks for suspicious patterns like high amounts, rapid transactions, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private static final BigDecimal MAX_SINGLE_TRANSACTION = new BigDecimal("10000.00");
    private static final int MAX_TRANSACTIONS_PER_HOUR = 10;
    private static final BigDecimal MAX_HOURLY_AMOUNT = new BigDecimal("50000.00");

    private final TransactionRepository transactionRepository;

    public void validateTransaction(ProcessPaymentRequest request) {
        log.debug("Running fraud detection for userId={}, amount={}", request.getUserId(), request.getAmount());

        // Check single transaction limit
        if (request.getAmount().compareTo(MAX_SINGLE_TRANSACTION) > 0) {
            log.warn("Fraud check failed: amount {} exceeds single transaction limit for userId={}",
                    request.getAmount(), request.getUserId());
            throw new FraudDetectionException(
                    "Transaction amount exceeds maximum allowed limit");
        }

        // Check transaction velocity (number of transactions per hour)
        var recentTransactions = transactionRepository.findRecentByUserId(
                request.getUserId(), PageRequest.of(0, MAX_TRANSACTIONS_PER_HOUR + 1));

        long recentCount = recentTransactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .count();

        if (recentCount >= MAX_TRANSACTIONS_PER_HOUR) {
            log.warn("Fraud check failed: userId={} exceeded {} transactions in the last hour",
                    request.getUserId(), MAX_TRANSACTIONS_PER_HOUR);
            throw new FraudDetectionException(
                    "Too many transactions in a short period. Please try again later.");
        }

        // Check hourly amount limit
        BigDecimal hourlyTotal = recentTransactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (hourlyTotal.add(request.getAmount()).compareTo(MAX_HOURLY_AMOUNT) > 0) {
            log.warn("Fraud check failed: userId={} hourly amount would exceed limit", request.getUserId());
            throw new FraudDetectionException(
                    "Hourly transaction amount limit exceeded. Please try again later.");
        }

        log.debug("Fraud detection passed for userId={}", request.getUserId());
    }
}
