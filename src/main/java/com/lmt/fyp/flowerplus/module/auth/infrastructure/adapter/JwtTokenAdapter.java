package com.lmt.fyp.flowerplus.module.auth.infrastructure.adapter;

import com.lmt.fyp.flowerplus.module.auth.application.port.out.TokenIssuerPort;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.security.JwtService;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenIssuerPort {
    private final JwtService jwtService;

    @Override
    public String issueAccessToken(UserJpaEntity user) {
        return jwtService.generateToken(SecurityUser.fromEntity(user));
    }
}