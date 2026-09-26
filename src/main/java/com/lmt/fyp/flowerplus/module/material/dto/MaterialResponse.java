package com.lmt.fyp.flowerplus.module.material.dto;

import com.lmt.fyp.flowerplus.module.material.entity.Material;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        String name,
        MaterialType type,
        UnitOfMeasure unitOfMeasure,
        BigDecimal sellingPrice,
        MaterialStatus status,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Long version
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getType(),
                material.getUnitOfMeasure(),
                material.getSellingPrice(),
                material.getStatus(),
                material.getCreatedAt(),
                material.getUpdatedAt(),
                material.getCreatedBy(),
                material.getUpdatedBy(),
                material.getVersion()
        );
    }
}
