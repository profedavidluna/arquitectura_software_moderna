package com.ecommerce.productservice.integration;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ReviewRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true", disabledReason = "Docker not available")
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String categoryId;
    private static String productId;

    @Test
    @Order(1)
    @DisplayName("Should create a category")
    void shouldCreateCategory() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic products and gadgets")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        categoryId = objectMapper.readTree(responseBody).get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("Should create a product")
    void shouldCreateProduct() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Wireless Headphones")
                .description("High-quality wireless headphones with noise cancellation")
                .shortDescription("Premium wireless headphones")
                .price(new BigDecimal("79.99"))
                .compareAtPrice(new BigDecimal("99.99"))
                .costPrice(new BigDecimal("35.00"))
                .categoryId(UUID.fromString(categoryId))
                .sku("WH-001")
                .barcode("1234567890123")
                .weight(new BigDecimal("0.25"))
                .weightUnit("kg")
                .tags(new String[]{"wireless", "headphones", "audio"})
                .isActive(true)
                .isFeatured(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wireless Headphones"))
                .andExpect(jsonPath("$.sku").value("WH-001"))
                .andExpect(jsonPath("$.price").value(79.99))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.isFeatured").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        productId = objectMapper.readTree(responseBody).get("id").asText();
    }

    @Test
    @Order(3)
    @DisplayName("Should get product by ID")
    void shouldGetProductById() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Wireless Headphones"))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));
    }

    @Test
    @Order(4)
    @DisplayName("Should list products with pagination")
    void shouldListProductsWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @Order(5)
    @DisplayName("Should search products")
    void shouldSearchProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("q", "wireless"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Wireless Headphones"));
    }

    @Test
    @Order(6)
    @DisplayName("Should get products by category")
    void shouldGetProductsByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/products/category/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryId").value(categoryId));
    }

    @Test
    @Order(7)
    @DisplayName("Should update product")
    void shouldUpdateProduct() throws Exception {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("Wireless Headphones Pro")
                .price(new BigDecimal("89.99"))
                .sku("WH-001")
                .categoryId(UUID.fromString(categoryId))
                .build();

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Headphones Pro"))
                .andExpect(jsonPath("$.price").value(89.99));
    }

    @Test
    @Order(8)
    @DisplayName("Should add a review")
    void shouldAddReview() throws Exception {
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .userId(UUID.randomUUID())
                .rating(5)
                .title("Excellent headphones")
                .reviewText("Best headphones I've ever owned!")
                .isVerifiedPurchase(true)
                .build();

        mockMvc.perform(post("/api/v1/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Excellent headphones"));
    }

    @Test
    @Order(9)
    @DisplayName("Should get product reviews")
    void shouldGetProductReviews() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(10)
    @DisplayName("Should soft delete product")
    void shouldSoftDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", productId))
                .andExpect(status().isNoContent());

        // Verify product is no longer in active list
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(11)
    @DisplayName("Should return 404 for non-existent product")
    void shouldReturn404ForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(12)
    @DisplayName("Should return 400 for invalid product request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .name("")  // blank name
                .price(new BigDecimal("-1"))  // negative price
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    @DisplayName("Should get hierarchical categories")
    void shouldGetHierarchicalCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .param("hierarchical", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
