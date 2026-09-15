package com.lmt.fyp.flowerplus.module.auth.exception;

import java.time.Duration;

/**
 * A new code was requested before the minimum interval had elapsed.
 *
 * <p>Applies to registration-triggered sends as well as explicit resends —
 * otherwise repeated registration attempts become a way to mail-bomb an
 * address the attacker does not own.
 */
public class OtpThrottledException extends RuntimeException {

    private final Duration retryAfter;

    public OtpThrottledException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    /** How long until the same request can succeed. */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
