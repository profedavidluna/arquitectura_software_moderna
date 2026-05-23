package com.ecommerce.product.domain.service;

import com.ecommerce.product.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Product Service Interface - Domain Layer
 * 
 * <p><b>SOLID - ISP (Interface Segregation Principle)</b>:
 * This interface defines only the operations relevant to product management.
 * Clients depend only on the methods they use.</p>
 * 
 * <p><b>SOLID - DIP (Dependency Inversion Principle)</b>:
 * High-level modules (controllers) depend on this abstraction,
 * not on the concrete implementation. The implementation can be
 * swapped without affecting consumers.</p>
 * 
 * <p><b>SOA - Service Contract</b>:
 * This interface defines the service contract that all implementations must fulfill.
 * It represents the capabilities exposed by the Product Service.</p>
 */
public interface ProductService {

    /**
     * Creates a new product in the catalog and publishes a ProductCreatedEvent.
     *
     * @param product the product to create
     * @return the created product with generated ID
     */
    Product createProduct(Product product);

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param id the product UUID
     * @return an Optional containing the product if found
     */
    Optional<Product> getProductById(UUID id);

    /**
     * Retrieves all active products in the catalog.
     *
     * @return list of active products
     */
    List<Product> getAllProducts();

    /**
     * Retrieves products filtered by category.
     *
     * @param category the category to filter by
     * @return list of products in the specified category
     */
    List<Product> getProductsByCategory(String category);

    /**
     * Updates an existing product's information.
     *
     * @param id the product UUID to update
     * @param product the updated product data
     * @return the updated product
     */
    Product updateProduct(UUID id, Product product);

    /**
     * Deactivates a product (soft delete).
     *
     * @param id the product UUID to deactivate
     */
    void deactivateProduct(UUID id);
}
