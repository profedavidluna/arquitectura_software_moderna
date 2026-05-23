package com.ecommerce.inventoryservice.integration;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import com.ecommerce.inventoryservice.repository.LowStockAlertRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("inventory_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private LowStockAlertRepository alertRepository;

    private static UUID productId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        alertRepository.deleteAll();
        inventoryRepository.deleteAll();
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Full inventory lifecycle: create -> reserve -> deplete -> restock")
    void fullInventoryLifecycle() throws Exception {
        // 1. Create inventory
        CreateInventoryRequest createRequest = CreateInventoryRequest.builder()
                .productId(productId)
                .sku("LIFECYCLE-SKU-001")
                .warehouseLocation("A-1-1")
                .quantityAvailable(100)
                .reorderPoint(20)
                .reorderQuantity(50)
                .maxQuantity(500)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantityAvailable").value(100))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"));

        // 2. Reserve stock
        ReserveStockRequest reserveRequest = ReserveStockRequest.builder()
                .quantity(10)
                .referenceId(UUID.randomUUID())
                .reason("Order reservation")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/reserve", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(90))
                .andExpect(jsonPath("$.quantityReserved").value(10));

        // 3. Deplete stock (ship order)
        DepleteStockRequest depleteRequest = DepleteStockRequest.builder()
                .quantity(10)
                .referenceId(UUID.randomUUID())
                .reason("Shipped")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/deplete", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(90))
                .andExpect(jsonPath("$.quantityReserved").value(0));

        // 4. Restock
        RestockRequest restockRequest = RestockRequest.builder()
                .quantity(50)
                .reason("Supplier delivery")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/restock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(140));

        // 5. Verify transaction history
        mockMvc.perform(get("/api/v1/inventory/{productId}/transactions", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));

        // Verify in database
        Inventory saved = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(saved.getQuantityAvailable()).isEqualTo(140);
        assertThat(saved.getQuantityReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("Low stock alert is created when stock falls below reorder point")
    void lowStockAlertCreation() throws Exception {
        // Create inventory with low stock
        CreateInventoryRequest createRequest = CreateInventoryRequest.builder()
                .productId(productId)
                .sku("LOW-STOCK-SKU")
                .quantityAvailable(25)
                .reorderPoint(20)
                .reorderQuantity(50)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Reserve stock to trigger low stock
        ReserveStockRequest reserveRequest = ReserveStockRequest.builder()
                .quantity(10)
                .reason("Order")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/reserve", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(15));

        // Verify alert was created
        mockMvc.perform(get("/api/v1/inventory/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.content[0].alertStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("Reconciliation creates adjustment transaction")
    void reconciliationTest() throws Exception {
        // Create inventory
        CreateInventoryRequest createRequest = CreateInventoryRequest.builder()
                .productId(productId)
                .sku("RECON-SKU")
                .quantityAvailable(100)
                .reorderPoint(10)
                .reorderQuantity(50)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Reconcile with different actual quantity
        ReconciliationRequest reconcileRequest = ReconciliationRequest.builder()
                .actualQuantity(95)
                .reason("Physical count discrepancy")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/reconcile", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reconcileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(95));

        // Verify adjustment transaction
        mockMvc.perform(get("/api/v1/inventory/{productId}/transactions", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionType").value("ADJUSTMENT"));
    }

    @Test
    @DisplayName("Reserve fails with insufficient stock")
    void reserveFailsWithInsufficientStock() throws Exception {
        // Create inventory with limited stock
        CreateInventoryRequest createRequest = CreateInventoryRequest.builder()
                .productId(productId)
                .sku("LIMITED-SKU")
                .quantityAvailable(5)
                .reorderPoint(10)
                .reorderQuantity(50)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Try to reserve more than available
        ReserveStockRequest reserveRequest = ReserveStockRequest.builder()
                .quantity(10)
                .reason("Order")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/reserve", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Release stock returns reserved to available")
    void releaseStockTest() throws Exception {
        // Create and reserve
        CreateInventoryRequest createRequest = CreateInventoryRequest.builder()
                .productId(productId)
                .sku("RELEASE-SKU")
                .quantityAvailable(100)
                .reorderPoint(10)
                .reorderQuantity(50)
                .build();

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        ReserveStockRequest reserveRequest = ReserveStockRequest.builder()
                .quantity(20)
                .reason("Order")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/reserve", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk());

        // Release
        ReleaseStockRequest releaseRequest = ReleaseStockRequest.builder()
                .quantity(20)
                .reason("Order cancelled")
                .build();

        mockMvc.perform(post("/api/v1/inventory/{productId}/release", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(releaseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(100))
                .andExpect(jsonPath("$.quantityReserved").value(0));
    }
}
