package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class InventoryServiceClient {

    private final RestClient restClient;

    public InventoryServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.inventory-service.url}") String inventoryServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(inventoryServiceUrl).build();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventoryService")
    public boolean reserveStock(UUID orderId, List<ReservationRequest> items) {
        log.info("Reserving stock for order: {}", orderId);
        ReservationResponse response = restClient.post()
                .uri("/api/v1/inventory/reserve")
                .body(new StockReservationRequest(orderId, items))
                .retrieve()
                .body(ReservationResponse.class);
        return response != null && response.isReserved();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseStockFallback")
    @Retry(name = "inventoryService")
    public void releaseStock(UUID orderId) {
        log.info("Releasing stock for order: {}", orderId);
        restClient.post()
                .uri("/api/v1/inventory/release/{orderId}", orderId)
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused")
    private boolean reserveStockFallback(UUID orderId, List<ReservationRequest> items, Throwable t) {
        log.error("Inventory service unavailable for order {}: {}", orderId, t.getMessage());
        throw new ServiceUnavailableException("Inventory Service", t);
    }

    @SuppressWarnings("unused")
    private void releaseStockFallback(UUID orderId, Throwable t) {
        log.error("Failed to release stock for order {}: {}", orderId, t.getMessage());
        throw new ServiceUnavailableException("Inventory Service", t);
    }
}
