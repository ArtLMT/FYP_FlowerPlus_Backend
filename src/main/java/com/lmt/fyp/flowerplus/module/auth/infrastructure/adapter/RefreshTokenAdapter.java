package com.lmt.fyp.flowerplus.module.auth.infrastructure.adapter;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaEntity;
import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaRepository;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Value("${application.security.jwt.refresh-expiration:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public RefreshTokenJpaEntity create(UserJpaEntity user) {
        RefreshTokenJpaEntity refreshToken = RefreshTokenJpaEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();

        return refreshTokenJpaRepository.save(refreshToken);
    }

    @Override
    public RefreshTokenJpaEntity verify(String token) {
        RefreshTokenJpaEntity refreshToken = refreshTokenJpaRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenJpaRepository.delete(refreshToken);
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenJpaRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenJpaRepository.save(rt);
                });
    }
}