package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.module.auth.application.exception.EmailUsedException;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.IssueOtpUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.RegisterUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.PasswordEncoderPort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.UserAccountPort;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterResponse;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final UserAccountPort userAccountPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IssueOtpUseCase issueOtpUseCase;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());

        Optional<UserJpaEntity> userAccount = userAccountPort.findByEmail(email);

        if (userAccount.isPresent()) {
            UserJpaEntity user = userAccount.get();
            if (user.getStatus() == UserAccountStatus.ACTIVE) {
                throw new EmailUsedException("Email already registered");
            } else if (user.getStatus() == UserAccountStatus.PENDING) {
                 user.setPassword(passwordEncoderPort.hash(request.getPassword()));

                 Optional<UserProfileJpaEntity> updatedProfile = userAccountPort.findProfileByUser(user);
                 updatedProfile.ifPresent(profile -> profile.setFullName(request.getFullName()));
                 userAccountPort.save(user);

                 issueOtpUseCase.issueOTP(email);

                 return RegisterResponse.builder()
                         .message("Please check your email")
                         .build();
            }
        }

        UserJpaEntity user = UserJpaEntity.builder()
                .username(email)
                .email(email)
                .password(passwordEncoderPort.hash(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .status(UserAccountStatus.PENDING)
                .provider(AuthProvider.LOCAL)
                .build();

        UserJpaEntity savedUser = userAccountPort.save(user);

        userAccountPort.saveProfile(UserProfileJpaEntity.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .build());

        issueOtpUseCase.issueOTP(email);

        return RegisterResponse.builder()
                .message("Please check your email")
                .build();
    }

}
