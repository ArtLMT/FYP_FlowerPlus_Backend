package com.lmt.fyp.flowerplus.module.product.dto;

import com.lmt.fyp.flowerplus.module.product.entity.Category;

import java.util.UUID;

/** Public category representation; deliberately omits the management version. */
public record CategoryResponse(UUID id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
