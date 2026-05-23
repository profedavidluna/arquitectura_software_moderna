package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.PaymentRetryLog;
import com.ecommerce.paymentservice.entity.enums.RetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRetryLogRepository extends JpaRepository<PaymentRetryLog, UUID> {

    List<PaymentRetryLog> findByTransactionId(UUID transactionId);

    @Query("SELECT MAX(r.attemptNumber) FROM PaymentRetryLog r WHERE r.transaction.id = :transactionId")
    Integer findMaxAttemptByTransactionId(@Param("transactionId") UUID transactionId);

    @Query("SELECT r FROM PaymentRetryLog r WHERE r.status = :status AND r.nextRetryAt <= :now")
    List<PaymentRetryLog> findPendingRetries(
            @Param("status") RetryStatus status,
            @Param("now") LocalDateTime now);
}
