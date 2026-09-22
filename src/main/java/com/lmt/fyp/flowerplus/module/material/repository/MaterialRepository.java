package com.lmt.fyp.flowerplus.module.material.repository;

import com.lmt.fyp.flowerplus.module.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

    @Query("SELECT COUNT(m) > 0 FROM Material m WHERE LOWER(TRIM(m.name)) = LOWER(TRIM(:name))")
    boolean existsByNameIgnoreCaseAndTrimmed(@Param("name") String name);

    @Query("SELECT COUNT(m) > 0 FROM Material m WHERE LOWER(TRIM(m.name)) = LOWER(TRIM(:name)) AND m.id != :id")
    boolean existsByNameIgnoreCaseAndTrimmedAndIdNot(@Param("name") String name, @Param("id") UUID id);
}
