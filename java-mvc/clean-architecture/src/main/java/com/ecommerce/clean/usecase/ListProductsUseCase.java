package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;

import java.math.BigDecimal;
import java.util.List;

/**
 * USE CASE: List and search products.
 */
public class ListProductsUseCase {

    private final ProductGateway productGateway;

    public ListProductsUseCase(ProductGateway productGateway) {
        this.productGateway = productGateway;
    }

    public List<Product> listAll(int page, int size) {
        int effectiveSize = Math.min(size, 100); // Business rule: max 100 per page
        return productGateway.findAll(page, effectiveSize);
    }

    public List<Product> search(String query, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return productGateway.search(query, category, minPrice, maxPrice);
    }
}
