package com.lmt.fyp.flowerplus.module.auth.application.port.in;

public interface LogoutUseCase {
    void logout(String refreshToken);
}
