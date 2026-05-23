package com.ecommerce.clean.interfaceadapter.gateway;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.framework.persistence.ProductJpaEntity;
import com.ecommerce.clean.framework.persistence.ProductJpaRepository;
import com.ecommerce.clean.usecase.port.ProductGateway;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * INTERFACE ADAPTER LAYER - Gateway implementation.
 * Converts between Entity layer objects and Framework layer objects.
 * Implements the use case output boundary.
 */
@Component
public class ProductDatabaseGateway implements ProductGateway {

    private final ProductJpaRepository jpaRepository;

    public ProductDatabaseGateway(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = toJpaEntity(product);
        ProductJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Product> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findByActiveTrue(pageRequest)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Product> search(String query, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return jpaRepository.search(query, category, minPrice, maxPrice)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepository.existsBySku(sku);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    // Mapping methods
    private ProductJpaEntity toJpaEntity(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setCategory(product.getCategory());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setSku(product.getSku());
        entity.setActive(product.isActive());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());
        return entity;
    }

    private Product toDomain(ProductJpaEntity entity) {
        Product product = new Product(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.getStockQuantity(),
                entity.getSku()
        );
        product.setCreatedAt(entity.getCreatedAt());
        product.setUpdatedAt(entity.getUpdatedAt());
        product.setActive(entity.isActive());
        return product;
    }
}
