package com.lmt.fyp.flowerplus.module.auth.service;

public interface EmailVerificationService {

    /** Consumes the code, activates the account and signs the user straight in. */
    TokenPair verifyEmail(String email, String code);

    /**
     * Re-issues a code to a still-pending account. Silent for unknown or
     * already-verified addresses, so the endpoint cannot be used to tell which
     * emails are registered.
     */
    void resend(String email);
}
