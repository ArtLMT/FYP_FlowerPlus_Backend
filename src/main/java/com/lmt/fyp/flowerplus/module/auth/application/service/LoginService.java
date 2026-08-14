package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.LoginUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.TokenIssuerPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.UserAccountPort;
import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.auth.web.dto.LoginRequest;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final UserAccountPort userAccountPort;
    private final TokenIssuerPort tokenIssuerPort;
    private final RefreshTokenPort refreshTokenPort;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserJpaEntity user = userAccountPort.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.USER_NOT_FOUND, "User not found with email: " + email));

        return AuthResponse.builder()
                .accessToken(tokenIssuerPort.issueAccessToken(user))
                .refreshToken(refreshTokenPort.create(user).getToken())
                .build();
    }
}