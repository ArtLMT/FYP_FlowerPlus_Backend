package com.lmt.fyp.flowerplus.module.email.application.port.in;

public interface SendEmailUseCase {
    void sendOTP(String email, String otp);

}