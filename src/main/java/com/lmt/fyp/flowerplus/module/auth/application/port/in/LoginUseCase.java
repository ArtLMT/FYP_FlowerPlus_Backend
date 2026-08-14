package com.lmt.fyp.flowerplus.module.auth.application.port.in;

import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.auth.web.dto.LoginRequest;

public interface LoginUseCase {

    AuthResponse login(LoginRequest request);
}
