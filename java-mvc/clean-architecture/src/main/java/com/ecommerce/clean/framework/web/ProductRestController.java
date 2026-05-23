package com.ecommerce.clean.framework.web;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.framework.web.dto.CreateProductRequest;
import com.ecommerce.clean.framework.web.dto.ProductResponse;
import com.ecommerce.clean.framework.web.dto.UpdateProductRequest;
import com.ecommerce.clean.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * FRAMEWORK LAYER - REST Controller.
 * Outermost layer. Depends on Use Cases (inner layer).
 * Translates HTTP into use case calls.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductRestController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final ListProductsUseCase listProductsUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ManageStockUseCase manageStockUseCase;

    public ProductRestController(CreateProductUseCase createProductUseCase,
                                  GetProductUseCase getProductUseCase,
                                  ListProductsUseCase listProductsUseCase,
                                  UpdateProductUseCase updateProductUseCase,
                                  DeleteProductUseCase deleteProductUseCase,
                                  ManageStockUseCase manageStockUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.listProductsUseCase = listProductsUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.manageStockUseCase = manageStockUseCase;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = createProductUseCase.execute(
                request.name(), request.description(), request.price(),
                request.category(), request.stockQuantity(), request.sku()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return getProductUseCase.execute(id)
                .map(p -> ResponseEntity.ok(ProductResponse.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ProductResponse> products = listProductsUseCase.listAll(page, size)
                .stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<ProductResponse> products = listProductsUseCase.search(query, category, minPrice, maxPrice)
                .stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateProductRequest request) {
        Product product = updateProductUseCase.execute(id, request.name(),
                request.description(), request.price(), request.category());
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock/decrease")
    public ResponseEntity<Void> decreaseStock(@PathVariable UUID id, @RequestParam int quantity) {
        manageStockUseCase.decrease(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/stock/increase")
    public ResponseEntity<Void> increaseStock(@PathVariable UUID id, @RequestParam int quantity) {
        manageStockUseCase.increase(id, quantity);
        return ResponseEntity.ok().build();
    }
}
