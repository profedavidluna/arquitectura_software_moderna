package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Transaction;
import com.ecommerce.paymentservice.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByOrderId(UUID orderId);

    Page<Transaction> findByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status, Pageable pageable);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    Optional<Transaction> findByGatewayTransactionId(String gatewayTransactionId);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt >= :since")
    List<Transaction> findByStatusSince(
            @Param("status") TransactionStatus status,
            @Param("since") LocalDateTime since);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'FAILED' AND t.createdAt >= :since")
    List<Transaction> findFailedTransactionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
