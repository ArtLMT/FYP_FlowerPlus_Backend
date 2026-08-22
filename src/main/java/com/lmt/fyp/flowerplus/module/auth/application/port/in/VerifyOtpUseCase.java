package com.lmt.fyp.flowerplus.module.auth.application.port.in;

public interface VerifyOtpUseCase {
    void verify(String email, String code);
}
