package com.lmt.fyp.flowerplus.module.material.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MaterialReferenceGuard {

    private final JdbcTemplate jdbcTemplate;

    public boolean isUsedByProductRecipe(UUID materialId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM product_recipe WHERE material_id = ?)",
                Boolean.class,
                materialId));
    }
}
