package com.lmt.fyp.flowerplus.module.email.service;

/**
 * Email operations published to the rest of the application. Callers depend on
 * this interface; the implementation lives in {@code service.impl.EmailServiceImpl}.
 */
public interface EmailService {

    void sendOTP(String email, String otp);
}
