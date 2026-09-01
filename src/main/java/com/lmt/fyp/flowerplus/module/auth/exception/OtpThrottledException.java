package com.lmt.fyp.flowerplus.module.auth.exception;

/**
 * A new code was requested before the minimum interval had elapsed.
 *
 * <p>Applies to registration-triggered sends as well as explicit resends —
 * otherwise repeated registration attempts become a way to mail-bomb an
 * address the attacker does not own.
 */
public class OtpThrottledException extends RuntimeException {
    public OtpThrottledException(String message) {
        super(message);
    }
}
