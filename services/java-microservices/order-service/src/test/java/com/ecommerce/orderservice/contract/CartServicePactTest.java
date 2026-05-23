package com.ecommerce.orderservice.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "cart-service")
class CartServicePactTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Pact(consumer = "order-service")
    public V4Pact getCartPact(PactDslWithProvider builder) {
        return builder
                .given("user has items in cart")
                .uponReceiving("a request to get user cart")
                .path("/api/v1/carts/user/" + USER_ID)
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .uuid("id")
                        .uuid("userId", USER_ID)
                        .decimalType("subtotal", 100.00)
                        .decimalType("taxAmount", 8.00)
                        .decimalType("shippingAmount", 5.99)
                        .decimalType("discountAmount", 0.00)
                        .decimalType("totalAmount", 113.99)
                        .eachLike("items", 1)
                            .uuid("productId")
                            .stringType("productName", "Test Product")
                            .stringType("productSku", "SKU-001")
                            .integerType("quantity", 2)
                            .decimalType("unitPrice", 50.00)
                            .decimalType("subtotal", 100.00)
                        .closeArray())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getCartPact")
    @DisplayName("Should get user cart from cart service")
    void shouldGetUserCart(MockServer mockServer) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.getUrl())
                .build();

        String response = restClient.get()
                .uri("/api/v1/carts/user/" + USER_ID)
                .retrieve()
                .body(String.class);

        assertThat(response).isNotNull();
        assertThat(response).contains("totalAmount");
        assertThat(response).contains("items");
    }

    @Pact(consumer = "order-service")
    public V4Pact clearCartPact(PactDslWithProvider builder) {
        return builder
                .given("user has items in cart")
                .uponReceiving("a request to clear user cart")
                .path("/api/v1/carts/user/" + USER_ID)
                .method("DELETE")
                .willRespondWith()
                .status(204)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "clearCartPact")
    @DisplayName("Should clear user cart after order creation")
    void shouldClearUserCart(MockServer mockServer) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.getUrl())
                .build();

        restClient.delete()
                .uri("/api/v1/carts/user/" + USER_ID)
                .retrieve()
                .toBodilessEntity();

        // If no exception thrown, the contract is satisfied
    }
}
