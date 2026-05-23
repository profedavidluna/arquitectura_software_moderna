package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    @Size(max = 120, message = "Slug must not exceed 120 characters")
    private String slug;

    private String description;

    private UUID parentId;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private Integer sortOrder;

    private Boolean isActive;
}
