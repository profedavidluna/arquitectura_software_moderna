package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.*;
import com.ecommerce.inventoryservice.event.InventoryEventPublisher;
import com.ecommerce.inventoryservice.exception.DuplicateInventoryException;
import com.ecommerce.inventoryservice.exception.InsufficientStockException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.mapper.InventoryMapper;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.InventoryTransactionRepository;
import com.ecommerce.inventoryservice.repository.LowStockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private LowStockAlertRepository alertRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;
    private Inventory inventory;
    private InventoryResponse inventoryResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .sku("SKU-001")
                .warehouseLocation("A-1-1")
                .quantityAvailable(100)
                .quantityReserved(10)
                .reorderPoint(20)
                .reorderQuantity(50)
                .maxQuantity(500)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inventoryResponse = InventoryResponse.builder()
                .id(inventory.getId())
                .productId(productId)
                .sku("SKU-001")
                .warehouseLocation("A-1-1")
                .quantityAvailable(100)
                .quantityReserved(10)
                .totalQuantity(110)
                .reorderPoint(20)
                .reorderQuantity(50)
                .stockStatus("IN_STOCK")
                .build();
    }

    @Nested
    @DisplayName("getByProductId")
    class GetByProductId {

        @Test
        @DisplayName("should return inventory when product exists")
        void shouldReturnInventory() {
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryMapper.toResponse(inventory)).thenReturn(inventoryResponse);

            InventoryResponse result = inventoryService.getByProductId(productId);

            assertThat(result).isNotNull();
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getQuantityAvailable()).isEqualTo(100);
        }

        @Test
        @DisplayName("should throw InventoryNotFoundException when product not found")
        void shouldThrowNotFound() {
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getByProductId(productId))
                    .isInstanceOf(InventoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createInventory")
    class CreateInventory {

        @Test
        @DisplayName("should create inventory successfully")
        void shouldCreateInventory() {
            CreateInventoryRequest request = CreateInventoryRequest.builder()
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .reorderPoint(20)
                    .reorderQuantity(50)
                    .build();

            when(inventoryRepository.existsByProductId(productId)).thenReturn(false);
            when(inventoryMapper.toEntity(request)).thenReturn(inventory);
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(inventoryMapper.toResponse(inventory)).thenReturn(inventoryResponse);

            InventoryResponse result = inventoryService.createInventory(request);

            assertThat(result).isNotNull();
            assertThat(result.getProductId()).isEqualTo(productId);
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("should throw DuplicateInventoryException when product already exists")
        void shouldThrowDuplicate() {
            CreateInventoryRequest request = CreateInventoryRequest.builder()
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .build();

            when(inventoryRepository.existsByProductId(productId)).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.createInventory(request))
                    .isInstanceOf(DuplicateInventoryException.class);
        }
    }

    @Nested
    @DisplayName("reserveStock")
    class ReserveStock {

        @Test
        @DisplayName("should reserve stock successfully")
        void shouldReserveStock() {
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .quantity(5)
                    .referenceId(UUID.randomUUID())
                    .reason("Order reservation")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .id(inventory.getId())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(95)
                    .quantityReserved(15)
                    .reorderPoint(20)
                    .reorderQuantity(50)
                    .build();

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(95)
                    .quantityReserved(15)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(updatedResponse);
            when(transactionRepository.save(any(InventoryTransaction.class))).thenReturn(null);

            InventoryResponse result = inventoryService.reserveStock(productId, request);

            assertThat(result.getQuantityAvailable()).isEqualTo(95);
            assertThat(result.getQuantityReserved()).isEqualTo(15);
            verify(eventPublisher).publishReserved(eq(productId), anyString(), eq(5), anyInt(), anyInt(), any());
            verify(transactionRepository).save(any(InventoryTransaction.class));
        }

        @Test
        @DisplayName("should throw InsufficientStockException when not enough stock")
        void shouldThrowInsufficientStock() {
            ReserveStockRequest request = ReserveStockRequest.builder()
                    .quantity(200)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.reserveStock(productId, request))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Nested
    @DisplayName("releaseStock")
    class ReleaseStock {

        @Test
        @DisplayName("should release stock successfully")
        void shouldReleaseStock() {
            ReleaseStockRequest request = ReleaseStockRequest.builder()
                    .quantity(5)
                    .referenceId(UUID.randomUUID())
                    .reason("Order cancelled")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .id(inventory.getId())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(105)
                    .quantityReserved(5)
                    .reorderPoint(20)
                    .reorderQuantity(50)
                    .build();

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(105)
                    .quantityReserved(5)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(updatedResponse);
            when(transactionRepository.save(any(InventoryTransaction.class))).thenReturn(null);

            InventoryResponse result = inventoryService.releaseStock(productId, request);

            assertThat(result.getQuantityAvailable()).isEqualTo(105);
            assertThat(result.getQuantityReserved()).isEqualTo(5);
            verify(transactionRepository).save(any(InventoryTransaction.class));
        }

        @Test
        @DisplayName("should throw when no reserved stock to release")
        void shouldThrowWhenNoReservedStock() {
            inventory.setQuantityReserved(0);

            ReleaseStockRequest request = ReleaseStockRequest.builder()
                    .quantity(5)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.releaseStock(productId, request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("depleteStock")
    class DepleteStock {

        @Test
        @DisplayName("should deplete stock successfully")
        void shouldDepleteStock() {
            DepleteStockRequest request = DepleteStockRequest.builder()
                    .quantity(5)
                    .referenceId(UUID.randomUUID())
                    .reason("Shipped")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .id(inventory.getId())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(100)
                    .quantityReserved(5)
                    .reorderPoint(20)
                    .build();

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(100)
                    .quantityReserved(5)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(updatedResponse);
            when(transactionRepository.save(any(InventoryTransaction.class))).thenReturn(null);

            InventoryResponse result = inventoryService.depleteStock(productId, request);

            assertThat(result.getQuantityReserved()).isEqualTo(5);
            verify(eventPublisher).publishDepleted(eq(productId), anyString(), eq(5), anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("should throw when insufficient reserved stock to deplete")
        void shouldThrowInsufficientReserved() {
            DepleteStockRequest request = DepleteStockRequest.builder()
                    .quantity(20)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.depleteStock(productId, request))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Nested
    @DisplayName("restockInventory")
    class RestockInventory {

        @Test
        @DisplayName("should restock successfully")
        void shouldRestock() {
            RestockRequest request = RestockRequest.builder()
                    .quantity(50)
                    .reason("Supplier delivery")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .id(inventory.getId())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(150)
                    .quantityReserved(10)
                    .reorderPoint(20)
                    .reorderQuantity(50)
                    .lastRestockedAt(LocalDateTime.now())
                    .build();

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(150)
                    .quantityReserved(10)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(updatedResponse);
            when(transactionRepository.save(any(InventoryTransaction.class))).thenReturn(null);

            InventoryResponse result = inventoryService.restockInventory(productId, request);

            assertThat(result.getQuantityAvailable()).isEqualTo(150);
            verify(transactionRepository).save(any(InventoryTransaction.class));
        }

        @Test
        @DisplayName("should throw when restock exceeds max quantity")
        void shouldThrowWhenExceedsMax() {
            RestockRequest request = RestockRequest.builder()
                    .quantity(500)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.restockInventory(productId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed max quantity");
        }
    }

    @Nested
    @DisplayName("reconcile")
    class Reconcile {

        @Test
        @DisplayName("should reconcile with positive adjustment")
        void shouldReconcilePositive() {
            ReconciliationRequest request = ReconciliationRequest.builder()
                    .actualQuantity(120)
                    .reason("Physical count")
                    .build();

            Inventory updatedInventory = Inventory.builder()
                    .id(inventory.getId())
                    .productId(productId)
                    .sku("SKU-001")
                    .quantityAvailable(120)
                    .quantityReserved(10)
                    .reorderPoint(20)
                    .build();

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .productId(productId)
                    .quantityAvailable(120)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(updatedResponse);
            when(transactionRepository.save(any(InventoryTransaction.class))).thenReturn(null);

            InventoryResponse result = inventoryService.reconcile(productId, request);

            assertThat(result.getQuantityAvailable()).isEqualTo(120);
            verify(transactionRepository).save(any(InventoryTransaction.class));
        }

        @Test
        @DisplayName("should skip reconciliation when no adjustment needed")
        void shouldSkipWhenNoAdjustment() {
            ReconciliationRequest request = ReconciliationRequest.builder()
                    .actualQuantity(100)
                    .build();

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
            when(inventoryMapper.toResponse(inventory)).thenReturn(inventoryResponse);

            InventoryResponse result = inventoryService.reconcile(productId, request);

            assertThat(result).isNotNull();
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getLowStockItems")
    class GetLowStockItems {

        @Test
        @DisplayName("should return low stock items")
        void shouldReturnLowStockItems() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Inventory> page = new PageImpl<>(List.of(inventory));

            when(inventoryRepository.findLowStockItems(pageable)).thenReturn(page);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(inventoryResponse);

            Page<InventoryResponse> result = inventoryService.getLowStockItems(pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("checkAvailability")
    class CheckAvailability {

        @Test
        @DisplayName("should return true when stock is available")
        void shouldReturnTrueWhenAvailable() {
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            boolean result = inventoryService.checkAvailability(productId, 50);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when stock is insufficient")
        void shouldReturnFalseWhenInsufficient() {
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            boolean result = inventoryService.checkAvailability(productId, 200);

            assertThat(result).isFalse();
        }
    }
}
