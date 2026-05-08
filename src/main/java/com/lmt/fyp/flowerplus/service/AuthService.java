package com.lmt.fyp.flowerplus.service;

import com.lmt.fyp.flowerplus.dto.request.LoginRequest;

public interface AuthService {
    void register(LoginRequest request);
}
