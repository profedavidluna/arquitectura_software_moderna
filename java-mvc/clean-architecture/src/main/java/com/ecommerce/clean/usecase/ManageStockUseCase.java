package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;

import java.util.UUID;

/**
 * USE CASE: Manage product stock (increase/decrease).
 */
public class ManageStockUseCase {

    private final ProductGateway productGateway;

    public ManageStockUseCase(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public void decrease(UUID productId, int quantity) {
        Product product = productGateway.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.decreaseStock(quantity);
        productGateway.save(product);
    }

    public void increase(UUID productId, int quantity) {
        Product product = productGateway.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.increaseStock(quantity);
        productGateway.save(product);
    }
}
