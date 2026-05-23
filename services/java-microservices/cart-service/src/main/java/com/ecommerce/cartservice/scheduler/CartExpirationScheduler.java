package com.ecommerce.cartservice.scheduler;

import com.ecommerce.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartExpirationScheduler {

    private final CartRepository cartRepository;

    /**
     * Runs every hour to mark expired carts as abandoned.
     * Carts expire 30 days after creation by default.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void markExpiredCarts() {
        log.info("Running cart expiration check...");
        int expiredCount = cartRepository.markExpiredCartsAsAbandoned(LocalDateTime.now());
        if (expiredCount > 0) {
            log.info("Marked {} expired carts as abandoned", expiredCount);
        } else {
            log.debug("No expired carts found");
        }
    }
}
