package com.lmt.fyp.flowerplus.service;

import com.lmt.fyp.flowerplus.dto.request.LoginRequest;
import com.lmt.fyp.flowerplus.dto.request.RegisterRequest;
import com.lmt.fyp.flowerplus.dto.response.AuthResponse;

public interface AuthService {

    /** Registers a new user and returns a JWT token immediately. */
    AuthResponse register(RegisterRequest request);

    /** Authenticates an existing user and returns a JWT token. */
    AuthResponse login(LoginRequest request);
}
