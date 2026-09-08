package com.lmt.fyp.flowerplus.module.auth.event;

/**
 * Published when an account's email verification succeeds and the activation
 * has been written. Consumed AFTER_COMMIT to invalidate the used code, so a
 * code is spent only once activation is durable — mirrors {@link OtpRequestedEvent},
 * which defers the OTP email the same way.
 */
public record EmailVerifiedEvent(String email) { }
