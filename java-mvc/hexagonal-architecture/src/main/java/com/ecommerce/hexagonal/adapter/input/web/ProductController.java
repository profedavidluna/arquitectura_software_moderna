package com.ecommerce.hexagonal.adapter.input.web;

import com.ecommerce.hexagonal.adapter.input.web.dto.CreateProductRequest;
import com.ecommerce.hexagonal.adapter.input.web.dto.ProductResponse;
import com.ecommerce.hexagonal.adapter.input.web.dto.UpdateProductRequest;
import com.ecommerce.hexagonal.domain.model.Product;
import com.ecommerce.hexagonal.domain.port.input.ProductServicePort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input Adapter (Driving Adapter) - REST Controller.
 * Translates HTTP requests into domain port calls.
 * Depends on the INPUT PORT (ProductServicePort), not the implementation.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductServicePort productService;

    public ProductController(ProductServicePort productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(
                request.name(),
                request.description(),
                request.price(),
                request.category(),
                request.stockQuantity(),
                request.sku()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromDomain(product));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(ProductResponse.fromDomain(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ProductResponse> products = productService.getAllProducts(page, size)
                .stream()
                .map(ProductResponse::fromDomain)
                .toList();
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
                .map(ProductResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        Product product = productService.updateProduct(id, request.name(),
                request.description(), request.price(), request.category());
        return ResponseEntity.ok(ProductResponse.fromDomain(product));
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
