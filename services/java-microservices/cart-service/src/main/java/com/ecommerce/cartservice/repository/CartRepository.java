package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    List<Cart> findByUserId(UUID userId);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.expiresAt < :now")
    List<Cart> findExpiredCarts(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Cart c SET c.status = 'ABANDONED' WHERE c.status = 'ACTIVE' AND c.expiresAt < :now")
    int markExpiredCartsAsAbandoned(@Param("now") LocalDateTime now);

    Optional<Cart> findBySessionIdAndStatus(String sessionId, CartStatus status);
}
