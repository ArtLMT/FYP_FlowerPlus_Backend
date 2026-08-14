package com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence;

import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByToken(String token);

    void deleteByUser(UserJpaEntity user);

    void deleteByExpiryDateBefore(Instant expiryDate);
}
