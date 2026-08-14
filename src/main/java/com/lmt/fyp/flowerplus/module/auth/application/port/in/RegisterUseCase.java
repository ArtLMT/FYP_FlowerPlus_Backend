package com.lmt.fyp.flowerplus.module.auth.application.port.in;

import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;

public interface RegisterUseCase {
    AuthResponse register(RegisterRequest request);
}
