package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;

import java.math.BigDecimal;

/**
 * USE CASE LAYER - Application Business Rules.
 * Each use case is a single class with a single responsibility.
 * Depends ONLY on Entity layer and Gateway interfaces.
 * NO framework dependencies.
 */
public class CreateProductUseCase {

    private final ProductGateway productGateway;

    public CreateProductUseCase(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public Product execute(String name, String description, BigDecimal price,
                           String category, int stockQuantity, String sku) {
        // Application-level business rule: SKU uniqueness
        if (sku != null && !sku.isBlank() && productGateway.existsBySku(sku)) {
            throw new IllegalArgumentException("Product with SKU '" + sku + "' already exists");
        }

        // Entity creation (entity validates its own invariants)
        Product product = new Product(null, name, description, price, category, stockQuantity, sku);

        return productGateway.save(product);
    }
}
