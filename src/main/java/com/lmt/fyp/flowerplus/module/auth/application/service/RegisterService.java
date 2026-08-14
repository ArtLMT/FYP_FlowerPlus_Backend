package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.module.auth.application.exception.EmailUsedException;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.RegisterUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.PasswordEncoderPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.TokenIssuerPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.UserAccountPort;
import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final UserAccountPort userAccountPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenIssuerPort tokenIssuerPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());

        if (userAccountPort.existsByEmail(email)) {
            throw new EmailUsedException("Email already registered: " + email);
        }

        UserJpaEntity user = UserJpaEntity.builder()
                .username(email)
                .email(email)
                .password(passwordEncoderPort.hash(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build();

        UserJpaEntity savedUser = userAccountPort.save(user);

        userAccountPort.saveProfile(UserProfileJpaEntity.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .build());

        return AuthResponse.builder()
                .accessToken(tokenIssuerPort.issueAccessToken(savedUser))
                .refreshToken(refreshTokenPort.create(savedUser).getToken())
                .build();
    }

}
