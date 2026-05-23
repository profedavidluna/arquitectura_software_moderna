package com.ecommerce.inventoryservice.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "InventoryService")
class InventoryConsumerPactTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Pact(consumer = "OrderService")
    public V4Pact getInventoryPact(PactDslWithProvider builder) {
        return builder
                .given("inventory exists for product")
                .uponReceiving("a request to get inventory for a product")
                .path("/api/v1/inventory/550e8400-e29b-41d4-a716-446655440000")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("id")
                        .uuid("productId", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                        .stringType("sku", "SKU-001")
                        .integerType("quantityAvailable", 100)
                        .integerType("quantityReserved", 10)
                        .integerType("totalQuantity", 110)
                        .stringType("stockStatus", "IN_STOCK"))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "OrderService")
    public V4Pact reserveStockPact(PactDslWithProvider builder) {
        return builder
                .given("inventory exists for product with sufficient stock")
                .uponReceiving("a request to reserve stock")
                .path("/api/v1/inventory/550e8400-e29b-41d4-a716-446655440000/reserve")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .integerType("quantity", 5)
                        .uuid("referenceId")
                        .stringType("reason", "Order reservation"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("productId", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                        .integerType("quantityAvailable")
                        .integerType("quantityReserved"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getInventoryPact")
    void testGetInventory(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/v1/inventory/550e8400-e29b-41d4-a716-446655440000";
        var response = restTemplate.getForEntity(url, Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("quantityAvailable");
        assertThat(response.getBody().get("stockStatus")).isEqualTo("IN_STOCK");
    }

    @Test
    @PactTestFor(pactMethod = "reserveStockPact")
    void testReserveStock(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/v1/inventory/550e8400-e29b-41d4-a716-446655440000/reserve";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                    "quantity": 5,
                    "referenceId": "660e8400-e29b-41d4-a716-446655440000",
                    "reason": "Order reservation"
                }
                """;

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity(url, request, Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("quantityAvailable");
        assertThat(response.getBody()).containsKey("quantityReserved");
    }
}
