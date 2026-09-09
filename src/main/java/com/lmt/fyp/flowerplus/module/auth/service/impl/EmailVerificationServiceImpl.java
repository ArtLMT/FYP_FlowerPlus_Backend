package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.event.EmailVerifiedEvent;
import com.lmt.fyp.flowerplus.module.auth.service.EmailVerificationService;
import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;
import com.lmt.fyp.flowerplus.module.auth.service.OtpService;
import com.lmt.fyp.flowerplus.module.auth.service.RefreshTokenService;
import com.lmt.fyp.flowerplus.module.auth.service.TokenPair;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import com.lmt.fyp.flowerplus.security.JwtService;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserService userService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TokenPair verifyEmail(String email, String code) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        otpService.verify(OtpPurpose.REGISTRATION, normalizedEmail, code);

        User user = userService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.USER_NOT_FOUND, "User not found with email: " + normalizedEmail));

        // Dirty checking: the status change flushes when this transaction commits.
        userService.activate(user);

        // Consume the code only once activation is durably committed. The
        // listener fires AFTER_COMMIT, so if this transaction rolls back the
        // code stays live and the user can retry rather than being stranded.
        eventPublisher.publishEvent(new EmailVerifiedEvent(OtpPurpose.REGISTRATION, normalizedEmail));

        return new TokenPair(
                jwtService.generateToken(SecurityUser.fromEntity(user)),
                refreshTokenService.create(user).getToken());
    }

    @Override
    public void resend(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        userService.findByEmail(normalizedEmail)
                .filter(user -> user.getStatus() == UserAccountStatus.PENDING)
                .ifPresent(user -> otpService.issueOTP(OtpPurpose.REGISTRATION, normalizedEmail));
    }
}
