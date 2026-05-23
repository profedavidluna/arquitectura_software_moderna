package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;

import java.util.UUID;

/**
 * USE CASE: Soft-delete a product (deactivate).
 */
public class DeleteProductUseCase {

    private final ProductGateway productGateway;

    public DeleteProductUseCase(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public void execute(UUID id) {
        Product product = productGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.deactivate();
        productGateway.save(product);
    }
}
