package com.lmt.fyp.flowerplus.module.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Database coordination for category deletion and the Product rows it affects. */
@Repository
@RequiredArgsConstructor
public class CategoryProductGuard {

    private final JdbcTemplate jdbcTemplate;

    /** Caller locks the Category first; Product rows are then locked in stable UUID order. */
    public List<UUID> lockProductsForCategory(UUID categoryId) {
        return jdbcTemplate.query("""
                        SELECT p.id
                        FROM product p
                        JOIN product_category pc ON pc.product_id = p.id
                        WHERE pc.category_id = ?
                        ORDER BY p.id
                        FOR UPDATE OF p
                        """,
                (resultSet, rowNum) -> resultSet.getObject(1, UUID.class), categoryId);
    }

    public List<UUID> findActiveProductsThatWouldBeOrphaned(UUID categoryId) {
        return jdbcTemplate.query("""
                        SELECT p.id
                        FROM product p
                        JOIN product_category pc ON pc.product_id = p.id
                        WHERE pc.category_id = ?
                          AND p.status = 'ACTIVE'
                          AND (SELECT COUNT(*) FROM product_category remaining
                               WHERE remaining.product_id = p.id) = 1
                        ORDER BY p.id
                        """,
                (resultSet, rowNum) -> resultSet.getObject(1, UUID.class), categoryId);
    }

    public void recordCategoryUnlink(List<UUID> productIds, UUID actorId) {
        if (productIds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "UPDATE product SET version = version + 1, updated_at = CURRENT_TIMESTAMP, updated_by = ? WHERE id = ?",
                productIds,
                productIds.size(),
                (statement, productId) -> {
                    if (actorId == null) {
                        statement.setNull(1, java.sql.Types.OTHER);
                    } else {
                        statement.setObject(1, actorId);
                    }
                    statement.setObject(2, productId);
                });
    }

    public void unlinkCategory(UUID categoryId) {
        jdbcTemplate.update("DELETE FROM product_category WHERE category_id = ?", categoryId);
    }
}
