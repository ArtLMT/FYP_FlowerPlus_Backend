package com.lmt.fyp.flowerplus.module.material.repository;

import com.lmt.fyp.flowerplus.module.material.entity.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Material m WHERE m.id = :id")
    Optional<Material> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Material m WHERE m.id IN :ids ORDER BY m.id")
    List<Material> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

    @Query("SELECT COUNT(m) > 0 FROM Material m WHERE LOWER(m.name) = LOWER(:name)")
    boolean existsByNormalizedNameIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(m) > 0 FROM Material m WHERE LOWER(m.name) = LOWER(:name) AND m.id != :id")
    boolean existsByNormalizedNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") UUID id);
}
