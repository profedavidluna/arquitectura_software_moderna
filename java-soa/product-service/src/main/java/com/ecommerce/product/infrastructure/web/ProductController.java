package com.ecommerce.product.infrastructure.web;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.service.ProductService;
import com.ecommerce.product.infrastructure.web.dto.CreateProductRequest;
import com.ecommerce.product.infrastructure.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product REST Controller - Infrastructure Layer (Web Adapter)
 * 
 * <p><b>Adapter Pattern</b>: This controller is an "input adapter" that translates
 * HTTP requests into domain operations. It adapts the web protocol to the
 * service interface.</p>
 * 
 * <p><b>SOA - Service Endpoint</b>: This is the service endpoint that exposes
 * the Product Service capabilities via REST. In SOA, services are accessed
 * through well-defined endpoints with standardized protocols.</p>
 * 
 * <p><b>SOLID - SRP</b>: This controller only handles HTTP concerns:
 * request parsing, response formatting, and status codes.
 * Business logic is delegated to the service layer.</p>
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * POST /api/products - Create a new product
     * 
     * <p>Creates a product and triggers a ProductCreatedEvent on the ESB.</p>
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("REST: Create product request received: name={}", request.getName());

        Product product = Product.create(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.getSku()
        );

        Product created = productService.createProduct(product);
        ProductResponse response = ProductResponse.fromDomain(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/products - Get all active products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String category) {

        List<Product> products;
        if (category != null && !category.isBlank()) {
            products = productService.getProductsByCategory(category);
        } else {
            products = productService.getAllProducts();
        }

        List<ProductResponse> response = products.stream()
                .map(ProductResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/{id} - Get a product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return productService.getProductById(id)
                .map(product -> ResponseEntity.ok(ProductResponse.fromDomain(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/products/{id} - Update a product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {

        Product product = Product.create(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.getSku()
        );

        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(ProductResponse.fromDomain(updated));
    }

    /**
     * DELETE /api/products/{id} - Deactivate a product (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable UUID id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }
}
