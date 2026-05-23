package com.ecommerce.cartservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.product-service.url:http://localhost:8082}")
    private String productServiceUrl;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    public Optional<ProductInfo> getProduct(UUID productId) {
        log.debug("Fetching product info for productId: {}", productId);
        String url = productServiceUrl + "/api/v1/products/" + productId;
        ProductInfo product = restTemplate.getForObject(url, ProductInfo.class);
        return Optional.ofNullable(product);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "checkStockFallback")
    @Retry(name = "productService")
    public boolean checkStock(UUID productId, int quantity) {
        log.debug("Checking stock for productId: {}, quantity: {}", productId, quantity);
        String url = productServiceUrl + "/api/v1/products/" + productId + "/stock?quantity=" + quantity;
        Boolean available = restTemplate.getForObject(url, Boolean.class);
        return Boolean.TRUE.equals(available);
    }

    @SuppressWarnings("unused")
    private Optional<ProductInfo> getProductFallback(UUID productId, Throwable t) {
        log.warn("Circuit breaker fallback for getProduct. ProductId: {}, Error: {}", productId, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private boolean checkStockFallback(UUID productId, int quantity, Throwable t) {
        log.warn("Circuit breaker fallback for checkStock. ProductId: {}, Error: {}", productId, t.getMessage());
        return true; // Optimistic fallback - allow adding to cart
    }
}
