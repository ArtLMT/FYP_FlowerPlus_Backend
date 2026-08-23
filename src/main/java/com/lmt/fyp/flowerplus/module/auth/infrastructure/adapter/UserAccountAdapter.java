package com.lmt.fyp.flowerplus.module.auth.infrastructure.adapter;

import com.lmt.fyp.flowerplus.module.auth.application.port.out.UserAccountPort;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaRepository;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAccountAdapter implements UserAccountPort {

    private final UserJpaRepository userJpaRepository;
    private final UserProfileJpaRepository userProfileJpaRepository;

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.findByEmail(email).isPresent();
    }

    @Override
    public Optional<UserJpaEntity> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public UserJpaEntity save(UserJpaEntity user) {
        return userJpaRepository.save(user);
    }

    @Override
    public UserProfileJpaEntity saveProfile(UserProfileJpaEntity profile) {
        return userProfileJpaRepository.save(profile);
    }

    @Override
    public Optional<UserProfileJpaEntity> findProfileByUser(UserJpaEntity user) {
        return userProfileJpaRepository.findByUser(user);
    }
}
