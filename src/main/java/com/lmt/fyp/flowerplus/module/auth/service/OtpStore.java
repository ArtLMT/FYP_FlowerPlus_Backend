package com.lmt.fyp.flowerplus.module.auth.service;

import java.time.Duration;
import java.util.Optional;

/**
 * The seam onto wherever outstanding codes live.
 *
 * <p>This interface earns its keep: the production store is Redis, and faking
 * it is how the OTP rules are tested without a running server.
 */
public interface OtpStore {
    /** Stores the hash as the sole outstanding code for (purpose, email), resetting attempts to zero. */
    void save(OtpPurpose purpose, String email, String codeHash, Duration ttl);

    /** The outstanding code hash, or empty if none is live (never issued, expired, or consumed). */
    Optional<String> findHash(OtpPurpose purpose, String email);

    /** Records one failed guess and returns the new total. Must be atomic. */
    long incrementAttempts(OtpPurpose purpose, String email);

    /** Drops the outstanding code, whether it was consumed or burnt through. */
    void invalidate(OtpPurpose purpose, String email);

    /**
     * Claims the right to send a code for (purpose, email), blocking further
     * sends for {@code interval}. Must be atomic — two concurrent requests may
     * not both succeed.
     *
     * @return false if a send is still within the cooldown
     */
    boolean tryAcquireResendSlot(OtpPurpose purpose, String email, Duration interval);

    /**
     * Counts one send for (purpose, email) and returns the total in the current
     * window, which opens at the first send and lasts {@code window}. Must be atomic.
     */
    long incrementSendCount(OtpPurpose purpose, String email, Duration window);

    /** Time left on the resend cooldown for (purpose, email); zero if none is running. */
    Duration resendCooldownRemaining(OtpPurpose purpose, String email);

    /** Time left in the current send-count window for (purpose, email); zero if none is open. */
    Duration sendWindowRemaining(OtpPurpose purpose, String email);
}
