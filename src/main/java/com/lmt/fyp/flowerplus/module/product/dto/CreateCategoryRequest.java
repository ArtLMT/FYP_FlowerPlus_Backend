package com.lmt.fyp.flowerplus.module.product.dto;

import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name
) {

    public CreateCategoryRequest {
        name = StringNormalizer.strip(name);
    }
}
