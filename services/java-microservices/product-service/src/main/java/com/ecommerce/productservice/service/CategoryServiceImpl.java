package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.exception.DuplicateResourceException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.mapper.ProductMapper;
import com.ecommerce.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        Category category = productMapper.toEntity(request);

        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(generateSlug(request.getName()));
        }

        if (categoryRepository.existsBySlug(category.getSlug())) {
            throw new DuplicateResourceException("Category", "slug", category.getSlug());
        }

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getParentId()));
            category.setParent(parent);
        }

        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created with ID: {}", saved.getId());

        return productMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        log.debug("Fetching category by ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return productMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all active categories");
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        return productMapper.toCategoryResponseList(categories);
    }

    @Override
    public List<CategoryResponse> getHierarchicalCategories() {
        log.debug("Fetching hierarchical categories");
        List<Category> rootCategories = categoryRepository.findRootCategoriesWithChildren();
        return productMapper.toCategoryResponseList(rootCategories);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        log.info("Updating category with ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Category", "name", request.getName());
            }
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getParentId()));
            category.setParent(parent);
        }

        productMapper.updateEntity(request, category);
        Category saved = categoryRepository.save(category);
        log.info("Category updated with ID: {}", saved.getId());

        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        log.info("Deleting category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Category soft deleted with ID: {}", id);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
