package com.lmt.fyp.flowerplus.module.product.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRecipeLineRequest(
        @NotNull(message = "Material id is required")
        UUID materialId,

        @NotNull(message = "Recipe quantity is required")
        @Positive(message = "Recipe quantity must be positive")
        @Digits(integer = 10, fraction = 2, message = "Recipe quantity must have at most two decimal places")
        BigDecimal quantityRequired
) {
}
