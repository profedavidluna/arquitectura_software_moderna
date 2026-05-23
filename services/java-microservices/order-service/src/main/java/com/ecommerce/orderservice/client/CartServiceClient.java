package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@Slf4j
public class CartServiceClient {

    private final RestClient restClient;

    public CartServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.cart-service.url}") String cartServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(cartServiceUrl).build();
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "getCartFallback")
    @Retry(name = "cartService")
    public CartResponse getCart(UUID userId) {
        log.info("Fetching cart for user: {}", userId);
        return restClient.get()
                .uri("/api/v1/carts/user/{userId}", userId)
                .retrieve()
                .body(CartResponse.class);
    }

    @CircuitBreaker(name = "cartService", fallbackMethod = "clearCartFallback")
    @Retry(name = "cartService")
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);
        restClient.delete()
                .uri("/api/v1/carts/user/{userId}", userId)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused")
    private CartResponse getCartFallback(UUID userId, Throwable t) {
        log.error("Cart service unavailable for user {}: {}", userId, t.getMessage());
        throw new ServiceUnavailableException("Cart Service", t);
    }

    @SuppressWarnings("unused")
    private void clearCartFallback(UUID userId, Throwable t) {
        log.error("Failed to clear cart for user {}: {}", userId, t.getMessage());
        throw new ServiceUnavailableException("Cart Service", t);
    }
}
