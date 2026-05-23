package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * USE CASE: Update product details.
 */
public class UpdateProductUseCase {

    private final ProductGateway productGateway;

    public UpdateProductUseCase(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public Product execute(UUID id, String name, String description, BigDecimal price, String category) {
        Product product = productGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.updateDetails(name, description, price, category);
        return productGateway.save(product);
    }
}
