package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.service.CategoryService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryResponse testResponse;
    private CategoryRequest testRequest;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        testResponse = CategoryResponse.builder()
                .id(categoryId)
                .name("Electronics")
                .slug("electronics")
                .description("Electronic products")
                .isActive(true)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic products")
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/categories")
    class CreateCategory {

        @Test
        @DisplayName("Should create category and return 201")
        void shouldCreateCategory() throws Exception {
            when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(testResponse);

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(categoryId.toString()))
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.slug").value("electronics"));
        }

        @Test
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CategoryRequest invalidRequest = CategoryRequest.builder().build();

            mockMvc.perform(post("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories")
    class ListCategories {

        @Test
        @DisplayName("Should return hierarchical categories")
        void shouldReturnHierarchicalCategories() throws Exception {
            when(categoryService.getHierarchicalCategories()).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/v1/categories")
                            .param("hierarchical", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("Electronics"));
        }

        @Test
        @DisplayName("Should return flat categories list")
        void shouldReturnFlatCategories() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/v1/categories")
                            .param("hierarchical", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categories/{id}")
    class GetCategoryById {

        @Test
        @DisplayName("Should return category by ID")
        void shouldReturnCategoryById() throws Exception {
            when(categoryService.getCategoryById(categoryId)).thenReturn(testResponse);

            mockMvc.perform(get("/api/v1/categories/{id}", categoryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(categoryId.toString()))
                    .andExpect(jsonPath("$.name").value("Electronics"));
        }

        @Test
        @DisplayName("Should return 404 when category not found")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryService.getCategoryById(id))
                    .thenThrow(new ResourceNotFoundException("Category", "id", id));

            mockMvc.perform(get("/api/v1/categories/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/categories/{id}")
    class UpdateCategory {

        @Test
        @DisplayName("Should update category")
        void shouldUpdateCategory() throws Exception {
            when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                    .thenReturn(testResponse);

            mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(categoryId.toString()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categories/{id}")
    class DeleteCategory {

        @Test
        @DisplayName("Should delete category and return 204")
        void shouldDeleteCategory() throws Exception {
            doNothing().when(categoryService).deleteCategory(categoryId);

            mockMvc.perform(delete("/api/v1/categories/{id}", categoryId))
                    .andExpect(status().isNoContent());
        }
    }
}
