package com.ecommerce.layered.business;

import com.ecommerce.layered.business.exception.DuplicateSkuException;
import com.ecommerce.layered.business.exception.InsufficientStockException;
import com.ecommerce.layered.business.exception.ProductNotFoundException;
import com.ecommerce.layered.business.service.ProductService;
import com.ecommerce.layered.data.entity.ProductEntity;
import com.ecommerce.layered.data.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void shouldCreateProduct() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> {
            ProductEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ProductEntity result = productService.createProduct(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-001"
        );

        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    void shouldRejectDuplicateSku() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(
                "Laptop", "Desc", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-001"
        )).isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldDecreaseStock() {
        UUID id = UUID.randomUUID();
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setStockQuantity(10);

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productService.decreaseStock(id, 3);

        verify(productRepository).save(argThat(p -> p.getStockQuantity() == 7));
    }

    @Test
    void shouldRejectInsufficientStock() {
        UUID id = UUID.randomUUID();
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setStockQuantity(5);

        when(productRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> productService.decreaseStock(id, 10))
                .isInstanceOf(InsufficientStockException.class);
    }
}
