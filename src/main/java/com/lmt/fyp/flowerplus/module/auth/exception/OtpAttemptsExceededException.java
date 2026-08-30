package com.lmt.fyp.flowerplus.module.auth.exception;

/**
 * The attempt cap for one code was reached, so that code was destroyed.
 *
 * <p>Only the code dies — the account is neither locked nor deleted. The user
 * recovers by requesting a new one.
 */
public class OtpAttemptsExceededException extends RuntimeException {
    public OtpAttemptsExceededException(String message) {
        super(message);
    }
}
