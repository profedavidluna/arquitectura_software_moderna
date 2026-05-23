package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryRequest testRequest;
    private CategoryResponse testResponse;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(UUID.randomUUID())
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

        testResponse = CategoryResponse.builder()
                .id(testCategory.getId())
                .name("Electronics")
                .slug("electronics")
                .description("Electronic products")
                .isActive(true)
                .sortOrder(0)
                .build();
    }

    @Nested
    @DisplayName("Create Category")
    class CreateCategory {

        @Test
        @DisplayName("Should create category successfully")
        void shouldCreateCategorySuccessfully() {
            when(categoryRepository.existsByName("Electronics")).thenReturn(false);
            when(productMapper.toEntity(testRequest)).thenReturn(testCategory);
            when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(productMapper.toResponse(testCategory)).thenReturn(testResponse);

            CategoryResponse result = categoryService.createCategory(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Electronics");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw exception when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            when(categoryRepository.existsByName("Electronics")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(testRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("Should create category with parent")
        void shouldCreateCategoryWithParent() {
            Category parent = Category.builder()
                    .id(UUID.randomUUID())
                    .name("Parent")
                    .slug("parent")
                    .build();

            testRequest.setParentId(parent.getId());

            when(categoryRepository.existsByName("Electronics")).thenReturn(false);
            when(productMapper.toEntity(testRequest)).thenReturn(testCategory);
            when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
            when(categoryRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(productMapper.toResponse(testCategory)).thenReturn(testResponse);

            CategoryResponse result = categoryService.createCategory(testRequest);

            assertThat(result).isNotNull();
            verify(categoryRepository).findById(parent.getId());
        }
    }

    @Nested
    @DisplayName("Get Category")
    class GetCategory {

        @Test
        @DisplayName("Should get category by ID")
        void shouldGetCategoryById() {
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
            when(productMapper.toResponse(testCategory)).thenReturn(testResponse);

            CategoryResponse result = categoryService.getCategoryById(testCategory.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testCategory.getId());
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            UUID id = UUID.randomUUID();
            when(categoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Categories")
    class ListCategories {

        @Test
        @DisplayName("Should return all active categories")
        void shouldReturnAllActiveCategories() {
            when(categoryRepository.findByIsActiveTrueOrderBySortOrderAsc())
                    .thenReturn(List.of(testCategory));
            when(productMapper.toCategoryResponseList(List.of(testCategory)))
                    .thenReturn(List.of(testResponse));

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return hierarchical categories")
        void shouldReturnHierarchicalCategories() {
            when(categoryRepository.findRootCategoriesWithChildren())
                    .thenReturn(List.of(testCategory));
            when(productMapper.toCategoryResponseList(List.of(testCategory)))
                    .thenReturn(List.of(testResponse));

            List<CategoryResponse> result = categoryService.getHierarchicalCategories();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update Category")
    class UpdateCategory {

        @Test
        @DisplayName("Should update category successfully")
        void shouldUpdateCategorySuccessfully() {
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
            when(productMapper.toResponse(testCategory)).thenReturn(testResponse);

            CategoryResponse result = categoryService.updateCategory(testCategory.getId(), testRequest);

            assertThat(result).isNotNull();
            verify(productMapper).updateEntity(testRequest, testCategory);
        }

        @Test
        @DisplayName("Should throw exception when setting self as parent")
        void shouldThrowExceptionWhenSelfParent() {
            testRequest.setParentId(testCategory.getId());
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

            assertThatThrownBy(() -> categoryService.updateCategory(testCategory.getId(), testRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("own parent");
        }
    }

    @Nested
    @DisplayName("Delete Category")
    class DeleteCategory {

        @Test
        @DisplayName("Should soft delete category")
        void shouldSoftDeleteCategory() {
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

            categoryService.deleteCategory(testCategory.getId());

            assertThat(testCategory.getIsActive()).isFalse();
            verify(categoryRepository).save(testCategory);
        }
    }
}
