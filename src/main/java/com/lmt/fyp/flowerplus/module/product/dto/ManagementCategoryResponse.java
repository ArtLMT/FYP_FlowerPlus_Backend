package com.lmt.fyp.flowerplus.module.product.dto;

import com.lmt.fyp.flowerplus.module.product.entity.Category;

import java.time.Instant;
import java.util.UUID;

public record ManagementCategoryResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Long version
) {

    public static ManagementCategoryResponse from(Category category) {
        return new ManagementCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getCreatedBy(),
                category.getUpdatedBy(),
                category.getVersion()
        );
    }
}
