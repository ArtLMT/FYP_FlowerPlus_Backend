package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.module.auth.application.port.in.LogoutUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenPort refreshTokenPort;

    @Override
    public void logout(String refreshToken) {
        refreshTokenPort.revoke(refreshToken);
    }
}
