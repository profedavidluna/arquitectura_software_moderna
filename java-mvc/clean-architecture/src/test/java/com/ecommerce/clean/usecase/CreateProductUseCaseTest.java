package com.ecommerce.clean.usecase;

import com.ecommerce.clean.entity.Product;
import com.ecommerce.clean.usecase.port.ProductGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for Use Case - NO framework dependencies.
 * Proves the use case is testable in isolation.
 */
@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    @Mock
    private ProductGateway productGateway;

    private CreateProductUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateProductUseCase(productGateway);
    }

    @Test
    void shouldCreateProduct() {
        when(productGateway.existsBySku("SKU-001")).thenReturn(false);
        when(productGateway.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = useCase.execute("Laptop", "Gaming", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-001");

        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(result.isActive()).isTrue();
        verify(productGateway).save(any(Product.class));
    }

    @Test
    void shouldRejectDuplicateSku() {
        when(productGateway.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute("Laptop", "Desc", new BigDecimal("999.99"),
                "Electronics", 10, "SKU-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectInvalidPrice() {
        assertThatThrownBy(() -> useCase.execute("Laptop", "Desc", new BigDecimal("-1"),
                "Electronics", 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price");
    }

    @Test
    void shouldRejectEmptyName() {
        assertThatThrownBy(() -> useCase.execute("", "Desc", new BigDecimal("10"),
                "Electronics", 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name");
    }
}
