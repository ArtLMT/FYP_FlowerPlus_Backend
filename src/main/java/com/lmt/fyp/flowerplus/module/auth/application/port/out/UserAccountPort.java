package com.lmt.fyp.flowerplus.module.auth.application.port.out;

import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaEntity;

import java.util.Optional;

public interface UserAccountPort {

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmail(String email);

    UserJpaEntity save(UserJpaEntity user);

    UserProfileJpaEntity saveProfile(UserProfileJpaEntity profile);

    Optional<UserProfileJpaEntity> findProfileByUser(UserJpaEntity user);
}
