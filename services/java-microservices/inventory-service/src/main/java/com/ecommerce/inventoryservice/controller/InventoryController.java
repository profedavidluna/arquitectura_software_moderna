package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management operations")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory for a product", description = "Retrieves current stock levels for a specific product")
    @ApiResponse(responseCode = "200", description = "Inventory found")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    public ResponseEntity<InventoryResponse> getInventory(
            @Parameter(description = "Product ID") @PathVariable UUID productId) {
        log.debug("GET /api/v1/inventory/{}", productId);
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @GetMapping
    @Operation(summary = "List all inventory", description = "Retrieves paginated list of all inventory records")
    @ApiResponse(responseCode = "200", description = "Inventory list retrieved")
    public ResponseEntity<Page<InventoryResponse>> getAllInventory(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/v1/inventory - page: {}", pageable.getPageNumber());
        return ResponseEntity.ok(inventoryService.getAllInventory(pageable));
    }

    @PostMapping
    @Operation(summary = "Create inventory record", description = "Creates a new inventory record for a product")
    @ApiResponse(responseCode = "201", description = "Inventory created")
    @ApiResponse(responseCode = "409", description = "Inventory already exists for product")
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        log.debug("POST /api/v1/inventory - productId: {}", request.getProductId());
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update inventory", description = "Updates inventory configuration (reorder point, warehouse location, etc.)")
    @ApiResponse(responseCode = "200", description = "Inventory updated")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    public ResponseEntity<InventoryResponse> updateInventory(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        log.debug("PUT /api/v1/inventory/{}", productId);
        return ResponseEntity.ok(inventoryService.updateInventory(productId, request));
    }

    @PostMapping("/{productId}/reserve")
    @Operation(summary = "Reserve stock", description = "Reserves stock for a pending order (atomic with optimistic locking)")
    @ApiResponse(responseCode = "200", description = "Stock reserved")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    @ApiResponse(responseCode = "409", description = "Insufficient stock or concurrent modification")
    public ResponseEntity<InventoryResponse> reserveStock(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody ReserveStockRequest request) {
        log.debug("POST /api/v1/inventory/{}/reserve - quantity: {}", productId, request.getQuantity());
        return ResponseEntity.ok(inventoryService.reserveStock(productId, request));
    }

    @PostMapping("/{productId}/release")
    @Operation(summary = "Release reserved stock", description = "Releases previously reserved stock back to available")
    @ApiResponse(responseCode = "200", description = "Stock released")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    public ResponseEntity<InventoryResponse> releaseStock(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody ReleaseStockRequest request) {
        log.debug("POST /api/v1/inventory/{}/release - quantity: {}", productId, request.getQuantity());
        return ResponseEntity.ok(inventoryService.releaseStock(productId, request));
    }

    @PostMapping("/{productId}/deplete")
    @Operation(summary = "Deplete stock", description = "Converts reserved stock to depleted after shipment")
    @ApiResponse(responseCode = "200", description = "Stock depleted")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    @ApiResponse(responseCode = "409", description = "Insufficient reserved stock")
    public ResponseEntity<InventoryResponse> depleteStock(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody DepleteStockRequest request) {
        log.debug("POST /api/v1/inventory/{}/deplete - quantity: {}", productId, request.getQuantity());
        return ResponseEntity.ok(inventoryService.depleteStock(productId, request));
    }

    @PostMapping("/{productId}/restock")
    @Operation(summary = "Restock inventory", description = "Adds stock to inventory")
    @ApiResponse(responseCode = "200", description = "Stock restocked")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    public ResponseEntity<InventoryResponse> restockInventory(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody RestockRequest request) {
        log.debug("POST /api/v1/inventory/{}/restock - quantity: {}", productId, request.getQuantity());
        return ResponseEntity.ok(inventoryService.restockInventory(productId, request));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Retrieves items where available quantity is at or below reorder point")
    @ApiResponse(responseCode = "200", description = "Low stock items retrieved")
    public ResponseEntity<Page<InventoryResponse>> getLowStockItems(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/v1/inventory/low-stock");
        return ResponseEntity.ok(inventoryService.getLowStockItems(pageable));
    }

    @GetMapping("/{productId}/transactions")
    @Operation(summary = "Get transaction history", description = "Retrieves transaction history for a product")
    @ApiResponse(responseCode = "200", description = "Transaction history retrieved")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/v1/inventory/{}/transactions", productId);
        return ResponseEntity.ok(inventoryService.getTransactionHistory(productId, pageable));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get active alerts", description = "Retrieves active and acknowledged low stock alerts")
    @ApiResponse(responseCode = "200", description = "Alerts retrieved")
    public ResponseEntity<Page<LowStockAlertResponse>> getActiveAlerts(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/v1/inventory/alerts");
        return ResponseEntity.ok(inventoryService.getActiveAlerts(pageable));
    }

    @PostMapping("/{productId}/reconcile")
    @Operation(summary = "Reconcile inventory", description = "Compares expected vs actual quantity and creates adjustment")
    @ApiResponse(responseCode = "200", description = "Inventory reconciled")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    public ResponseEntity<InventoryResponse> reconcileInventory(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody ReconciliationRequest request) {
        log.debug("POST /api/v1/inventory/{}/reconcile - actualQuantity: {}", productId, request.getActualQuantity());
        return ResponseEntity.ok(inventoryService.reconcile(productId, request));
    }
}
