package com.lmt.fyp.flowerplus.module.auth.event;

import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;

/**
 * Published when an account's email verification succeeds and the activation
 * has been written. Consumed AFTER_COMMIT to invalidate the used code, so a
 * code is spent only once activation is durable — mirrors {@link OtpRequestedEvent},
 * which defers the OTP email the same way. Carries the purpose so the right
 * namespaced code is invalidated.
 */
public record EmailVerifiedEvent(OtpPurpose purpose, String email) { }
