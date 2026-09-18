package com.lmt.fyp.flowerplus.module.email.service;

/**
 * Email operations published to the rest of the application. Callers depend on
 * this interface; the implementation lives in {@code service.impl.EmailServiceImpl}.
 */
public interface EmailService {

    void sendOTP(String email, String otp);

    void sendPasswordResetCode(String email, String otp);

    /** Welcome mail for a new Staff account, carrying the code that sets its first password. */
    void sendStaffInvitation(String email, String otp);
}
