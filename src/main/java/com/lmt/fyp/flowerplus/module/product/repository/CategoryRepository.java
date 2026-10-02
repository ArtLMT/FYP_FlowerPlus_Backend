package com.lmt.fyp.flowerplus.module.product.repository;

import com.lmt.fyp.flowerplus.module.product.entity.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("SELECT c FROM Category c ORDER BY LOWER(TRIM(c.name)), c.id")
    List<Category> findAllInDisplayOrder();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Category c WHERE c.id IN :ids ORDER BY c.id")
    List<Category> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
