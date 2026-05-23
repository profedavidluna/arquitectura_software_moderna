package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.config.SecurityConfig;
import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.exception.GlobalExceptionHandler;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    private final UUID productId = UUID.randomUUID();

    @Nested
    @DisplayName("GET /api/v1/inventory/{productId}")
    class GetInventory {

        @Test
        @DisplayName("should return 200 with inventory data")
        void shouldReturnInventory() throws Exception {
            InventoryResponse response = InventoryResponse.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .quantityReserved(10)
                    .totalQuantity(110)
                    .stockStatus("IN_STOCK")
                    .build();

            when(inventoryService.getByProductId(productId)).thenReturn(response);

            mockMvc.perform(get("/api/v1/inventory/{productId}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(productId.toString()))
                    .andExpect(jsonPath("$.quantityAvailable").value(100))
                    .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404() throws Exception {
            when(inventoryService.getByProductId(productId))
                    .thenThrow(new InventoryNotFoundException(productId));

            mockMvc.perform(get("/api/v1/inventory/{productId}", productId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/inventory")
    class GetAllInventory {

        @Test
        @DisplayName("should return paginated inventory list")
        void shouldReturnPaginatedList() throws Exception {
            InventoryResponse response = InventoryResponse.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .build();

            Page<InventoryResponse> page = new PageImpl<>(List.of(response));
            when(inventoryService.getAllInventory(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/inventory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory")
    class CreateInventory {

        @Test
        @DisplayName("should return 201 when inventory created")
        void shouldCreateInventory() throws Exception {
            CreateInventoryRequest request = CreateInventoryRequest.builder()
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .reorderPoint(20)
                    .reorderQuantity(50)
                    .build();

            InventoryResponse response = InventoryResponse.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .build();

            when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(productId.toString()));
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CreateInventoryRequest request = CreateInventoryRequest.builder()
                    .sku("SKU-001")
                    .quantityAvailable(-1)
                    .build();

            mockMvc.perform(post("/api/v1/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory/{productId}/reserve")
    class ReserveStock {

        @Test
        @DisplayName("should return 200 when stock reserved")
        void shouldReserveStock() throws Exception {
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .quantity(5)
                    .referenceId(UUID.randomUUID())
                    .reason("Order")
                    .build();

            InventoryResponse response = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(95)
                    .quantityReserved(15)
                    .build();

            when(inventoryService.reserveStock(eq(productId), any(ReserveStockRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/inventory/{productId}/reserve", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantityAvailable").value(95))
                    .andExpect(jsonPath("$.quantityReserved").value(15));
        }

        @Test
        @DisplayName("should return 409 when insufficient stock")
        void shouldReturn409() throws Exception {
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .quantity(200)
                    .build();

            when(inventoryService.reserveStock(eq(productId), any(ReserveStockRequest.class)))
                    .thenThrow(new InsufficientStockException(productId, 200, 100));

            mockMvc.perform(post("/api/v1/inventory/{productId}/reserve", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory/{productId}/deplete")
    class DepleteStock {

        @Test
        @DisplayName("should return 200 when stock depleted")
        void shouldDepleteStock() throws Exception {
            DepleteStockRequest request = DepleteStockRequest.builder()
                    .quantity(5)
                    .referenceId(UUID.randomUUID())
                    .reason("Shipped")
                    .build();

            InventoryResponse response = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(100)
                    .quantityReserved(5)
                    .build();

            when(inventoryService.depleteStock(eq(productId), any(DepleteStockRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/inventory/{productId}/deplete", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantityReserved").value(5));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory/{productId}/restock")
    class Restock {

        @Test
        @DisplayName("should return 200 when restocked")
        void shouldRestock() throws Exception {
            RestockRequest request = RestockRequest.builder()
                    .quantity(50)
                    .reason("Supplier delivery")
                    .build();

            InventoryResponse response = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(150)
                    .build();

            when(inventoryService.restockInventory(eq(productId), any(RestockRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/inventory/{productId}/restock", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantityAvailable").value(150));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/inventory/low-stock")
    class LowStock {

        @Test
        @DisplayName("should return low stock items")
        void shouldReturnLowStockItems() throws Exception {
            InventoryResponse response = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(5)
                    .reorderPoint(20)
                    .stockStatus("LOW_STOCK")
                    .build();

            Page<InventoryResponse> page = new PageImpl<>(List.of(response));
            when(inventoryService.getLowStockItems(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/inventory/low-stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].stockStatus").value("LOW_STOCK"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/inventory/{productId}/transactions")
    class TransactionHistory {

        @Test
        @DisplayName("should return transaction history")
        void shouldReturnTransactions() throws Exception {
            TransactionResponse txn = TransactionResponse.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .transactionType(com.ecommerce.inventoryservice.entity.TransactionType.RESERVE)
                    .quantity(5)
                    .quantityBefore(100)
                    .quantityAfter(95)
                    .build();

            Page<TransactionResponse> page = new PageImpl<>(List.of(txn));
            when(inventoryService.getTransactionHistory(eq(productId), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/inventory/{productId}/transactions", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].transactionType").value("RESERVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/inventory/alerts")
    class Alerts {

        @Test
        @DisplayName("should return active alerts")
        void shouldReturnAlerts() throws Exception {
            LowStockAlertResponse alert = LowStockAlertResponse.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .sku("SKU-001")
                    .threshold(20)
                    .currentQuantity(5)
                    .alertStatus(com.ecommerce.inventoryservice.entity.AlertStatus.ACTIVE)
                    .build();

            Page<LowStockAlertResponse> page = new PageImpl<>(List.of(alert));
            when(inventoryService.getActiveAlerts(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/inventory/alerts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].alertStatus").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inventory/{productId}/reconcile")
    class Reconcile {

        @Test
        @DisplayName("should return 200 when reconciled")
        void shouldReconcile() throws Exception {
            ReconciliationRequest request = ReconciliationRequest.builder()
                    .actualQuantity(95)
                    .reason("Physical count")
                    .build();

            InventoryResponse response = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(95)
                    .build();

            when(inventoryService.reconcile(eq(productId), any(ReconciliationRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/inventory/{productId}/reconcile", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quantityAvailable").value(95));
        }
    }
}
