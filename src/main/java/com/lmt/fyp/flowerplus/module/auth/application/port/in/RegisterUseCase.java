package com.lmt.fyp.flowerplus.module.auth.application.port.in;


import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterResponse;

public interface RegisterUseCase {
    RegisterResponse register(RegisterRequest request);
}
