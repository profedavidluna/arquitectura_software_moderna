package com.ecommerce.inventory.infrastructure.web;

import com.ecommerce.inventory.domain.model.InventoryItem;
import com.ecommerce.inventory.domain.service.InventoryService;
import com.ecommerce.inventory.infrastructure.web.dto.CreateInventoryRequest;
import com.ecommerce.inventory.infrastructure.web.dto.InventoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Inventory REST Controller - SOA Service Endpoint.
 *
 * <p>In SOA, each service exposes a well-defined interface (contract).
 * This controller defines the HTTP contract for the Inventory Service.</p>
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> addInventory(@Valid @RequestBody CreateInventoryRequest request) {
        InventoryItem item = inventoryService.createInventoryItem(
                request.productId(), request.productName(), request.quantityAvailable());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(item));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> listAll() {
        List<InventoryResponse> items = inventoryService.getAllInventory()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable UUID productId) {
        return inventoryService.getInventoryByProductId(productId)
                .map(item -> ResponseEntity.ok(toResponse(item)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{productId}/increase")
    public ResponseEntity<InventoryResponse> increaseStock(
            @PathVariable UUID productId, @RequestParam int quantity) {
        InventoryItem item = inventoryService.addStock(productId, quantity);
        return ResponseEntity.ok(toResponse(item));
    }

    private InventoryResponse toResponse(InventoryItem item) {
        return new InventoryResponse(
                item.getProductId(), item.getProductName(),
                item.getQuantityAvailable(), item.getQuantityReserved(),
                item.getUpdatedAt());
    }
}
