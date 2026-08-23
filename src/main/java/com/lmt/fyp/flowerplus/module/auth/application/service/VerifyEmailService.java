package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.VerifyEmailUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.VerifyOtpUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.TokenIssuerPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.UserAccountPort;
import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyEmailService implements VerifyEmailUseCase {
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final UserAccountPort userAccountPort;
    private final TokenIssuerPort tokenIssuerPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    @Transactional
    public AuthResponse verifyEmail(String email, String code) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        verifyOtpUseCase.verify(normalizedEmail, code);

        UserJpaEntity user = userAccountPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.USER_NOT_FOUND, "User not found with email: " + normalizedEmail));

        user.setStatus(UserAccountStatus.ACTIVE);

        return AuthResponse.builder()
                .accessToken(tokenIssuerPort.issueAccessToken(user))
                .refreshToken(refreshTokenPort.create(user).getToken())
                .build();
    }
}
