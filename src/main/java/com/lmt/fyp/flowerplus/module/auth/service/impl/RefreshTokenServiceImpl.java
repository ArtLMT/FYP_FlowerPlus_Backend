package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
import com.lmt.fyp.flowerplus.module.auth.service.RefreshTokenService;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${application.security.jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public RefreshToken create(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validate-and-rotate, all in one transaction.
     *
     * <p>noRollbackFor matters and — unlike the old verify() — actually works
     * here, because this method OWNS its transaction (the refresh use case is no
     * longer @Transactional, so nothing joins ahead of it). On reuse we delete
     * the family and then throw; noRollbackFor keeps that delete committed even
     * though the request is rejected — the delete is the whole point of the
     * rejection. Expiry is left to TokenCleanupScheduler, as before.
     */
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public RefreshToken rotate(String token) {
        RefreshToken current = refreshTokenRepository.findByTokenWithUser(token)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID));

        // Reuse detection: a token already retired (revoked) is being replayed.
        // Either someone is replaying a rotated token, or a logged-out token was
        // reused — treat the family as compromised and drop every token the user
        // holds, forcing a fresh login.
        if (current.isRevoked()) {
            refreshTokenRepository.deleteByUser(current.getUser());
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (current.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // Rotate: retire the presented token and issue a fresh one. The retired
        // row is kept (revoked) as a tombstone so a later replay is caught above;
        // TokenCleanupScheduler sweeps it.
        current.setRevoked(true);
        refreshTokenRepository.save(current);

        return refreshTokenRepository.save(RefreshToken.builder()
                .user(current.getUser())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build());
    }

    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
}
