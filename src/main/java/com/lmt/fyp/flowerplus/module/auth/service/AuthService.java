package com.lmt.fyp.flowerplus.module.auth.service;

import com.lmt.fyp.flowerplus.module.auth.dto.LoginRequest;
import com.lmt.fyp.flowerplus.module.auth.dto.RefreshTokenRequest;
import com.lmt.fyp.flowerplus.module.auth.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.auth.dto.AuthResponse;

public interface AuthService {

    /** Registers a new user and returns access + refresh tokens. */
    AuthResponse register(RegisterRequest request);

    /** Authenticates an existing user and returns access + refresh tokens. */
    AuthResponse login(LoginRequest request);

    /** Validates a refresh token and returns a new access token. */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /** Revokes/invalidates the refresh token on logout. */
    void logout(RefreshTokenRequest request);
}
