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
    /** Stores the hash as the sole outstanding code for the email, resetting attempts to zero. */
    void save(String email, String codeHash, Duration ttl);

    /** The outstanding code hash, or empty if none is live (never issued, expired, or consumed). */
    Optional<String> findHash(String email);

    /** Records one failed guess and returns the new total. Must be atomic. */
    long incrementAttempts(String email);

    /** Drops the outstanding code, whether it was consumed or burnt through. */
    void invalidate(String email);

    /**
     * Claims the right to send a code to this address, blocking further sends
     * for {@code interval}. Must be atomic — two concurrent requests may not
     * both succeed.
     *
     * @return false if a send is still within the cooldown
     */
    boolean tryAcquireResendSlot(String email, Duration interval);
}
