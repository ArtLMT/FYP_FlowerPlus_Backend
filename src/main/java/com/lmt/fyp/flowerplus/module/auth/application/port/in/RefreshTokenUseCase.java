package com.lmt.fyp.flowerplus.module.auth.application.port.in;

import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;

public interface RefreshTokenUseCase {
    AuthResponse refreshToken(String refreshToken);
}