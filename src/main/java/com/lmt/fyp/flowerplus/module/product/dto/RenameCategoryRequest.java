package com.lmt.fyp.flowerplus.module.product.dto;

import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record RenameCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotNull
        @PositiveOrZero
        Long version
) {

    public RenameCategoryRequest {
        name = StringNormalizer.strip(name);
    }
}
