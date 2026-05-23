package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.PaymentMethod;
import com.ecommerce.paymentservice.entity.enums.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);

    List<PaymentMethod> findByUserIdAndMethodTypeAndIsActiveTrue(UUID userId, PaymentMethodType methodType);

    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndTokenAndIsActiveTrue(UUID userId, String token);
}
