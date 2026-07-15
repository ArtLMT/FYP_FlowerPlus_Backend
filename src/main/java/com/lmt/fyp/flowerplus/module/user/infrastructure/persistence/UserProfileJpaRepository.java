package com.lmt.fyp.flowerplus.module.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * SPRING DATA REPOSITORY — OUTSIDE the wall (infrastructure).
 */
@Repository
public interface UserProfileJpaRepository extends JpaRepository<UserProfileJpaEntity, UUID> {

    Optional<UserProfileJpaEntity> findByUser(UserJpaEntity user);
}
