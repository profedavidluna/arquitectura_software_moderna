package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;

import java.util.Optional;
import java.util.UUID;

/**
 * USE CASE: Get a single product by ID.
 */
public class GetProductUseCase {

    private final ProductGateway productGateway;

    public GetProductUseCase(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public Optional<Product> execute(UUID id) {
        return productGateway.findById(id);
    }
}
