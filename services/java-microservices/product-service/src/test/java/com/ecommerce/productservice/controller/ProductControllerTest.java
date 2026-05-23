package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductResponse testResponse;
    private ProductRequest testRequest;
    private UUID productId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        testResponse = ProductResponse.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .sku("TEST-001")
                .categoryId(categoryId)
                .categoryName("Electronics")
                .isActive(true)
                .isFeatured(false)
                .averageRating(4.5)
                .reviewCount(10L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = ProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("29.99"))
                .sku("TEST-001")
                .categoryId(categoryId)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @DisplayName("Should create product and return 201")
        void shouldCreateProduct() throws Exception {
            when(productService.createProduct(any(ProductRequest.class))).thenReturn(testResponse);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(productId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Product"))
                    .andExpect(jsonPath("$.sku").value("TEST-001"));
        }

        @Test
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            ProductRequest invalidRequest = ProductRequest.builder().build();

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("Should return product by ID")
        void shouldReturnProductById() throws Exception {
            when(productService.getProductById(productId)).thenReturn(testResponse);

            mockMvc.perform(get("/api/v1/products/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Product"));
        }

        @Test
        @DisplayName("Should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(productService.getProductById(id))
                    .thenThrow(new ResourceNotFoundException("Product", "id", id));

            mockMvc.perform(get("/api/v1/products/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class ListProducts {

        @Test
        @DisplayName("Should return paginated products")
        void shouldReturnPaginatedProducts() throws Exception {
            PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                    .content(List.of(testResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(productService.getAllProducts(any())).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/products")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].name").value("Test Product"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("Should update product")
        void shouldUpdateProduct() throws Exception {
            when(productService.updateProduct(eq(productId), any(ProductRequest.class))).thenReturn(testResponse);

            mockMvc.perform(put("/api/v1/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productId.toString()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("Should soft delete product and return 204")
        void shouldDeleteProduct() throws Exception {
            doNothing().when(productService).deleteProduct(productId);

            mockMvc.perform(delete("/api/v1/products/{id}", productId))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/search")
    class SearchProducts {

        @Test
        @DisplayName("Should search products")
        void shouldSearchProducts() throws Exception {
            PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                    .content(List.of(testResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(productService.searchProducts(eq("test"), any())).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/products/search")
                            .param("q", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Test Product"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/category/{categoryId}")
    class GetProductsByCategory {

        @Test
        @DisplayName("Should return products by category")
        void shouldReturnProductsByCategory() throws Exception {
            PagedResponse<ProductResponse> pagedResponse = PagedResponse.<ProductResponse>builder()
                    .content(List.of(testResponse))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(productService.getProductsByCategory(eq(categoryId), any())).thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/products/category/{categoryId}", categoryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].categoryId").value(categoryId.toString()));
        }
    }
}
