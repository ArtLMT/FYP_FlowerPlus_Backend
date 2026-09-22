package com.lmt.fyp.flowerplus.module.material.dto;

import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMaterialRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        MaterialType type,

        @NotNull
        UnitOfMeasure unitOfMeasure,

        @NotNull
        @Min(1)
        BigDecimal sellingPrice
) {}
