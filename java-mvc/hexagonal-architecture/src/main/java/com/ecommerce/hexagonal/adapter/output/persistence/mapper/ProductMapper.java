package com.ecommerce.hexagonal.adapter.output.persistence.mapper;

import com.ecommerce.hexagonal.adapter.output.persistence.entity.ProductJpaEntity;
import com.ecommerce.hexagonal.domain.model.Product;

/**
 * Mapper between domain model and JPA entity.
 * Keeps the domain model clean from persistence concerns.
 */
public class ProductMapper {

    private ProductMapper() {}

    public static ProductJpaEntity toJpaEntity(Product product) {
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

    public static Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCategory(),
                entity.getStockQuantity(),
                entity.getSku(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
