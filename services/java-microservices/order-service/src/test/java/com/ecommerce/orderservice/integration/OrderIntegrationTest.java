package com.ecommerce.orderservice.integration;

import com.ecommerce.orderservice.entity.*;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8180/realms/ecommerce");
        registry.add("services.cart-service.url", () -> "http://localhost:8083");
        registry.add("services.inventory-service.url", () -> "http://localhost:8085");
        registry.add("services.payment-service.url", () -> "http://localhost:8086");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository statusHistoryRepository;

    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        Order order = Order.builder()
                .orderNumber("ORD-20261105-001")
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("8.00"))
                .shippingAmount(new BigDecimal("5.99"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("113.99"))
                .currency("USD")
                .shippingAddress(Address.builder()
                        .street("123 Main St")
                        .city("Springfield")
                        .state("IL")
                        .zipCode("62701")
                        .country("US")
                        .build())
                .build();

        Order savedOrder = orderRepository.save(order);
        testOrderId = savedOrder.getId();
    }

    @Test
    @WithMockUser
    @DisplayName("Should retrieve order by ID from database")
    void shouldRetrieveOrderById() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20261105-001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(113.99));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 for non-existent order")
    void shouldReturn404ForNonExistentOrder() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/orders/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should list orders with pagination")
    void shouldListOrdersWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should verify database persistence")
    void shouldVerifyDatabasePersistence() {
        assertThat(orderRepository.count()).isEqualTo(1);
        Order order = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(order.getOrderNumber()).isEqualTo("ORD-20261105-001");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("113.99"));
    }

    @Test
    @DisplayName("Should verify order repository custom queries")
    void shouldVerifyCustomQueries() {
        Order order = orderRepository.findById(testOrderId).orElseThrow();

        // Test findByOrderNumber
        assertThat(orderRepository.findByOrderNumber("ORD-20261105-001")).isPresent();

        // Test existsByOrderNumber
        assertThat(orderRepository.existsByOrderNumber("ORD-20261105-001")).isTrue();
        assertThat(orderRepository.existsByOrderNumber("ORD-99999999-999")).isFalse();

        // Test countByUserId
        assertThat(orderRepository.countByUserId(order.getUserId())).isEqualTo(1);
    }
}
