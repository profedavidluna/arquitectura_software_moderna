package com.ecommerce.product.application;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.infrastructure.messaging.ProductEventPublisher;
import com.ecommerce.product.infrastructure.persistence.ProductPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for ProductServiceImpl
 * 
 * <p>These tests verify the service layer logic in isolation by mocking
 * the persistence adapter and event publisher dependencies.</p>
 * 
 * <p><b>Testing Strategy</b>: Unit tests mock external dependencies
 * to test business logic in isolation. Integration tests would use
 * real databases and Kafka (not shown here).</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductPersistenceAdapter persistenceAdapter;

    @Mock
    private ProductEventPublisher eventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.create(
                "Test Product",
                "A test product description",
                new BigDecimal("29.99"),
                "Electronics",
                "TEST-SKU-001"
        );
    }

    @Test
    @DisplayName("Should create product and publish event")
    void createProduct_ShouldSaveAndPublishEvent() {
        // Arrange
        when(persistenceAdapter.save(any(Product.class))).thenReturn(sampleProduct);
        doNothing().when(eventPublisher).publishProductCreated(any(Product.class));

        // Act
        Product result = productService.createProduct(sampleProduct);

        // Assert
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals("TEST-SKU-001", result.getSku());

        // Verify interactions
        verify(persistenceAdapter, times(1)).save(any(Product.class));
        verify(eventPublisher, times(1)).publishProductCreated(any(Product.class));
    }

    @Test
    @DisplayName("Should return product by ID")
    void getProductById_ShouldReturnProduct() {
        // Arrange
        UUID productId = sampleProduct.getId();
        when(persistenceAdapter.findById(productId)).thenReturn(Optional.of(sampleProduct));

        // Act
        Optional<Product> result = productService.getProductById(productId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Product", result.get().getName());
        verify(persistenceAdapter, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Should return empty when product not found")
    void getProductById_ShouldReturnEmpty_WhenNotFound() {
        // Arrange
        UUID productId = UUID.randomUUID();
        when(persistenceAdapter.findById(productId)).thenReturn(Optional.empty());

        // Act
        Optional<Product> result = productService.getProductById(productId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return all active products")
    void getAllProducts_ShouldReturnActiveProducts() {
        // Arrange
        Product product2 = Product.create("Product 2", "Desc", new BigDecimal("49.99"), "Books", "SKU-002");
        when(persistenceAdapter.findAllActive()).thenReturn(Arrays.asList(sampleProduct, product2));

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertEquals(2, result.size());
        verify(persistenceAdapter, times(1)).findAllActive();
    }

    @Test
    @DisplayName("Should return products by category")
    void getProductsByCategory_ShouldFilterByCategory() {
        // Arrange
        when(persistenceAdapter.findByCategory("Electronics"))
                .thenReturn(List.of(sampleProduct));

        // Act
        List<Product> result = productService.getProductsByCategory("Electronics");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategory());
    }

    @Test
    @DisplayName("Should deactivate product")
    void deactivateProduct_ShouldSetInactive() {
        // Arrange
        UUID productId = sampleProduct.getId();
        when(persistenceAdapter.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(persistenceAdapter.save(any(Product.class))).thenReturn(sampleProduct);

        // Act
        productService.deactivateProduct(productId);

        // Assert
        verify(persistenceAdapter, times(1)).save(any(Product.class));
        assertFalse(sampleProduct.isActive());
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent product")
    void deactivateProduct_ShouldThrow_WhenNotFound() {
        // Arrange
        UUID productId = UUID.randomUUID();
        when(persistenceAdapter.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> productService.deactivateProduct(productId));
    }
}
