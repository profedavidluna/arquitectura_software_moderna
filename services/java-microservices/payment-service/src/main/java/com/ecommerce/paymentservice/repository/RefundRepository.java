package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByTransactionId(UUID transactionId);

    List<Refund> findByOrderId(UUID orderId);

    List<Refund> findByStatus(RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.transaction.id = :transactionId AND r.status = 'COMPLETED'")
    BigDecimal getTotalRefundedForTransaction(@Param("transactionId") UUID transactionId);
}
