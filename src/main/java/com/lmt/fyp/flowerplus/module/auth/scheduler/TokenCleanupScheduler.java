package com.lmt.fyp.flowerplus.module.auth.scheduler;

import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    /**
     * Purges expired refresh tokens from the database.
     * Runs every day at midnight (UTC).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens...");
        try {
            refreshTokenJpaRepository.deleteByExpiryDateBefore(Instant.now());
            log.info("Expired refresh tokens successfully purged.");
        } catch (Exception e) {
            log.error("Failed to purge expired refresh tokens", e);
        }
    }
}
