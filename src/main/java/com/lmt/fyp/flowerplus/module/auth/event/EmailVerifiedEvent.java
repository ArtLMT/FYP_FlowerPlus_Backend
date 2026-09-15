package com.lmt.fyp.flowerplus.module.auth.event;

import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;

/**
 * Published once an action verified by an emailed code has been written — an
 * account activation or a password reset. Consumed AFTER_COMMIT to invalidate
 * the used code, so a code is spent only once the action is durable — mirrors
 * {@link OtpRequestedEvent}, which defers the OTP email the same way. Carries
 * the purpose so the right namespaced code is invalidated.
 */
public record EmailVerifiedEvent(OtpPurpose purpose, String email) { }
