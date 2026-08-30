package com.lmt.fyp.flowerplus.module.auth.service;

/**
 * Owns the whole life of a registration code: generation, expiry policy,
 * attempt limits, verification, invalidation and resend throttling.
 *
 * <p>Delivery is deliberately NOT its concern. It publishes an event and the
 * email module decides how to render and send it.
 */
public interface OtpService {

    void issueOTP(String email);

    void verify(String email, String code);
}
