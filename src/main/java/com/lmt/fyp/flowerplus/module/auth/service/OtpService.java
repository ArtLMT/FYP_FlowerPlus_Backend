package com.lmt.fyp.flowerplus.module.auth.service;

/**
 * Owns the whole life of a code: generation, expiry policy, attempt limits,
 * verification, invalidation and send limits.
 *
 * <p>Delivery is deliberately NOT its concern. It publishes an event and the
 * email module decides how to render and send it.
 */
public interface OtpService {

    /** Applies the send limits, then issues a code and publishes it for delivery. */
    void issueOTP(OtpPurpose purpose, String email);

    /**
     * Applies exactly the send limits {@link #issueOTP} would, without issuing
     * anything. A request that gets no code is then limited identically to one
     * that does, so the limit itself can't reveal whether an account exists.
     *
     * @throws com.lmt.fyp.flowerplus.module.auth.exception.OtpThrottledException if a limit is hit
     */
    void throttle(OtpPurpose purpose, String email);

    void verify(OtpPurpose purpose, String email, String code);

    /**
     * Issues a first-password code for a newly created Staff account and
     * publishes it for delivery as a welcome email. The code is stored under
     * {@link OtpPurpose#PASSWORD_RESET}, so the ordinary reset-password endpoint
     * sets the password — only the delivery wording differs. It spends exactly
     * the same send limits as a reset, so an invitation for an email that was
     * just sent a reset code is throttled (a documented, tolerated edge).
     *
     * @throws com.lmt.fyp.flowerplus.module.auth.exception.OtpThrottledException if a send limit is hit
     */
    void issueStaffInvitation(String email);
}
