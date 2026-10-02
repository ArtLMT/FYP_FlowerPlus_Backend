package com.lmt.fyp.flowerplus.module.product.dto;

import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import com.lmt.fyp.flowerplus.module.product.entity.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record UpdateProductRequest(
        @NotNull @PositiveOrZero Long version,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotNull(message = "Product type is required")
        ProductType type,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        @Pattern(regexp = "(?s).*\\S.*", message = "Description must not be blank")
        String description,

        @Positive(message = "Base price must be positive")
        @Digits(integer = 12, fraction = 0, message = "Base price must be a whole-VND amount with at most 12 digits")
        BigDecimal basePrice,

        @UniqueElements(message = "A category may be selected only once")
        List<@NotNull(message = "Category id is required") UUID> categoryIds,

        @UniqueElements(message = "A material may appear only once in a recipe")
        List<@NotNull @Valid CreateProductRecipeLineRequest> recipe
) {

    public UpdateProductRequest {
        name = StringNormalizer.strip(name);
        categoryIds = categoryIds == null ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(categoryIds));
        recipe = recipe == null ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(recipe));
    }
}
