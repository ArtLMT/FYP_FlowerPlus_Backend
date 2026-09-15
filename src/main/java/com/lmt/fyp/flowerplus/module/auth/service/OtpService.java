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
}
