package com.lmt.fyp.flowerplus.module.material.dto;

import com.lmt.fyp.flowerplus.common.util.StringNormalizer;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMaterialRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotNull
        MaterialType type,

        @NotNull
        UnitOfMeasure unitOfMeasure,

        @NotNull
        @Min(1)
        @Digits(integer = 12, fraction = 0)
        BigDecimal sellingPrice,

        @NotNull
        @PositiveOrZero
        Long version
) {

    public UpdateMaterialRequest {
        name = StringNormalizer.strip(name);
    }
}
