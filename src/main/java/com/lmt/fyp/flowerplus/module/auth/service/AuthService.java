package com.lmt.fyp.flowerplus.module.auth.service;

public interface AuthService {

    void register(String email, String rawPassword, String fullName);

    TokenPair login(String email, String rawPassword);

    void logout(String refreshToken);

    TokenPair refresh(String refreshToken);

    /**
     * Sends a reset code if the email belongs to an ACTIVE or SUSPENDED account.
     * Looks identical to the caller whatever the outcome.
     */
    void requestPasswordReset(String email);

    /**
     * Sets a new password using a valid reset code, then ends every session the
     * account has.
     *
     * @throws com.lmt.fyp.flowerplus.exception.ApiException
     *         {@code OTP_INVALID} if the code is wrong or expired
     */
    void resetPassword(String email, String code, String rawNewPassword);
}
