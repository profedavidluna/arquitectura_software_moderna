package com.ecommerce.layered.presentation.controller;

import com.ecommerce.layered.business.service.ProductService;
import com.ecommerce.layered.presentation.dto.CreateProductRequest;
import com.ecommerce.layered.presentation.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * PRESENTATION LAYER - REST Controller.
 * Handles HTTP requests and delegates to Business layer.
 * Dependencies flow DOWNWARD: Presentation → Business → Data.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        var entity = productService.createProduct(
                request.name(), request.description(), request.price(),
                request.category(), request.stockQuantity(), request.sku()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        var entity = productService.getProductById(id);
        return ResponseEntity.ok(ProductResponse.fromEntity(entity));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductResponse> products = productService.getAllProducts(page, size)
                .map(ProductResponse::fromEntity);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<ProductResponse> products = productService.searchProducts(query, category, minPrice, maxPrice)
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {
        var entity = productService.updateProduct(id, request.name(),
                request.description(), request.price(), request.category());
        return ResponseEntity.ok(ProductResponse.fromEntity(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock/decrease")
    public ResponseEntity<Void> decreaseStock(@PathVariable UUID id, @RequestParam int quantity) {
        productService.decreaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/stock/increase")
    public ResponseEntity<Void> increaseStock(@PathVariable UUID id, @RequestParam int quantity) {
        productService.increaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }
}
