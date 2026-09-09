package com.lmt.fyp.flowerplus.module.auth.scheduler;

import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
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

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Purges expired refresh tokens from the database.
     * Also purges revoked tombstones left by rotation and logout.
     * Runs every day at midnight (UTC).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Starting scheduled cleanup of spent refresh tokens...");
        try {
            refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
            refreshTokenRepository.deleteRevoked();
            log.info("Expired and revoked refresh tokens successfully purged.");
        } catch (Exception e) {
            log.error("Failed to purge spent refresh tokens", e);
        }
    }
}
