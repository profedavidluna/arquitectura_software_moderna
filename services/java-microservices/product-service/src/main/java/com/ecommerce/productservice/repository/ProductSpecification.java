package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Product> isFeatured() {
        return (root, query, cb) -> cb.isTrue(root.get("isFeatured"));
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> nameContains(String query) {
        return (root, criteriaQuery, cb) -> cb.like(
                cb.lower(root.get("name")),
                "%" + query.toLowerCase() + "%"
        );
    }

    public static Specification<Product> descriptionContains(String query) {
        return (root, criteriaQuery, cb) -> cb.like(
                cb.lower(root.get("description")),
                "%" + query.toLowerCase() + "%"
        );
    }

    public static Specification<Product> searchByText(String query) {
        return Specification.where(nameContains(query))
                .or(descriptionContains(query));
    }
}
