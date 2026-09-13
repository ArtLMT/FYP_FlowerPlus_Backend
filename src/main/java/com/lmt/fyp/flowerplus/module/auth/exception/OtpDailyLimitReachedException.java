package com.lmt.fyp.flowerplus.module.auth.exception;

import java.time.Duration;

/**
 * An address has already received the daily number of password-reset codes.
 *
 * <p>A kind of {@link OtpThrottledException}, so anything that handles
 * throttling still handles this; it has its own error code so the client can
 * tell "wait a minute" from "come back tomorrow".
 */
public class OtpDailyLimitReachedException extends OtpThrottledException {

    public OtpDailyLimitReachedException(String message, Duration retryAfter) {
        super(message, retryAfter);
    }
}
