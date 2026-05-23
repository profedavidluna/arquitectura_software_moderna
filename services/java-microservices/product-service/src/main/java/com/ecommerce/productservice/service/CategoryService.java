package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse getCategoryById(UUID id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getHierarchicalCategories();

    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    void deleteCategory(UUID id);
}
