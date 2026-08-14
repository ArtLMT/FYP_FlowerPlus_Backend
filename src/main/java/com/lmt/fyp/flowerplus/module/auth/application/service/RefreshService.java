package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.RefreshTokenUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.TokenIssuerPort;
import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaEntity;
import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshService implements RefreshTokenUseCase {
    private final RefreshTokenPort refreshTokenPort;
    private final TokenIssuerPort tokenIssuerPort;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        RefreshTokenJpaEntity stored = refreshTokenPort.verify(refreshToken);

        // A revoked account status must invalidate the session immediately,
        // not wait out the refresh token's remaining lifetime.
        UserJpaEntity user = stored.getUser();
        if (SecurityUser.isAuthBlocked(user.getStatus())) {
            throw new UnauthorizedException(
                    ErrorCode.REFRESH_TOKEN_INVALID, "Account is not permitted to refresh");
        }

        return AuthResponse.builder()
                .accessToken(tokenIssuerPort.issueAccessToken(user))
                .refreshToken(stored.getToken())
                .build();
    }
}
