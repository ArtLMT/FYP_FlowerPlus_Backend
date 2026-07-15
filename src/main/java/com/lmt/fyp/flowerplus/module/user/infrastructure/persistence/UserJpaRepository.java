package com.lmt.fyp.flowerplus.module.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * SPRING DATA REPOSITORY — OUTSIDE the wall (infrastructure).
 *
 * Speaks in JPA entities. The clean core never sees this type; only the
 * persistence adapter uses it, translating results into the domain model.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmailOrUsername(String email, String username);

    boolean existsByUsername(String username);
}
