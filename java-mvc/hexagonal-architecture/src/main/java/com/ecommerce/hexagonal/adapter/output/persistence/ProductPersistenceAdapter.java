package com.ecommerce.hexagonal.adapter.output.persistence;

import com.ecommerce.hexagonal.adapter.output.persistence.entity.ProductJpaEntity;
import com.ecommerce.hexagonal.adapter.output.persistence.mapper.ProductMapper;
import com.ecommerce.hexagonal.adapter.output.persistence.repository.SpringDataProductRepository;
import com.ecommerce.hexagonal.domain.model.Product;
import com.ecommerce.hexagonal.domain.port.output.ProductRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Adapter (Driven Adapter) - implements the output port.
 * Translates domain operations into JPA/database operations.
 * This is where Spring/JPA dependencies live.
 */
@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository jpaRepository;

    public ProductPersistenceAdapter(SpringDataProductRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = ProductMapper.toJpaEntity(product);
        ProductJpaEntity saved = jpaRepository.save(entity);
        return ProductMapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id).map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findByActiveTrue(pageRequest)
                .stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> search(String query, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return jpaRepository.search(query, category, minPrice, maxPrice)
                .stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepository.existsBySku(sku);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
