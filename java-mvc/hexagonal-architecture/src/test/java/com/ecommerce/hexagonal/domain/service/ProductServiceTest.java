package com.ecommerce.hexagonal.domain.service;

import com.ecommerce.hexagonal.domain.model.Product;
import com.ecommerce.hexagonal.domain.port.output.ProductRepositoryPort;
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

/**
 * Unit test for domain service.
 * Tests business logic WITHOUT any framework dependencies.
 * Uses mocked output port - proves domain is framework-independent.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void shouldCreateProduct() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product product = productService.createProduct(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-001"
        );

        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertThat(product.isActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldRejectDuplicateSku() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(
                "Laptop", "Gaming laptop", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-001"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already exists");
    }

    @Test
    void shouldDecreaseStock() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create("Laptop", "Desc", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-002");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.decreaseStock(productId, 3);

        verify(productRepository).save(argThat(p -> p.getStockQuantity() == 7));
    }

    @Test
    void shouldRejectDecreaseStockBeyondAvailable() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create("Laptop", "Desc", new BigDecimal("999.99"),
                "Electronics", 5, "SKU-003");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decreaseStock(productId, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void shouldLimitPageSizeTo100() {
        productService.getAllProducts(0, 200);
        verify(productRepository).findAll(0, 100);
    }
}
