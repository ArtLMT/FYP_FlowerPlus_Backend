package com.lmt.fyp.flowerplus.module.auth.application.service;

import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.config.OtpProperties;
import com.lmt.fyp.flowerplus.module.auth.application.exception.OtpAttemptsExceededException;
import com.lmt.fyp.flowerplus.module.auth.application.exception.OtpInvalidException;
import com.lmt.fyp.flowerplus.module.auth.application.exception.OtpThrottledException;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.IssueOtpUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.VerifyOtpUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.OtpStorePort;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.PasswordEncoderPort;
import com.lmt.fyp.flowerplus.module.email.application.event.OtpRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Owns the whole life of a registration code: generation, expiry policy,
 * attempt limits, verification, invalidation and resend throttling.
 *
 * <p>Delivery is deliberately NOT its concern. It publishes an event and the
 * email module decides how to render and send it, so this service never learns
 * what SMTP is and the email module never learns why a code was requested.
 */
@Service
@RequiredArgsConstructor
public class OtpService implements IssueOtpUseCase, VerifyOtpUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_ORIGIN = 100_000;
    private static final int CODE_BOUND = 900_000;

    private final OtpStorePort otpStorePort;
    private final PasswordEncoderPort passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final OtpProperties otpProperties;

    @Override
    public void issueOTP(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (!otpStorePort.tryAcquireResendSlot(normalizedEmail, otpProperties.resendInterval())) {
            throw new OtpThrottledException("A code was sent recently. Please wait before requesting another.");
        }

        String code = generateOTP();

        otpStorePort.save(normalizedEmail, passwordEncoder.hash(code), otpProperties.ttl());

        eventPublisher.publishEvent(new OtpRequestedEvent(normalizedEmail, code));
    }

    @Override
    public void verify(String email, String code) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        String storedHash = otpStorePort.findHash(normalizedEmail)
                .orElseThrow(() -> new OtpInvalidException("Verification code is incorrect or has expired."));

        long used = otpStorePort.incrementAttempts(normalizedEmail);

        if (used > otpProperties.maxAttempts()) {
            otpStorePort.invalidate(normalizedEmail);
            throw new OtpAttemptsExceededException("Too many incorrect attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(code, storedHash)) {
            throw new OtpInvalidException("Verification code is incorrect or has expired.");
        }

        otpStorePort.invalidate(normalizedEmail);
    }

    private String generateOTP() {
        return String.valueOf(CODE_ORIGIN + SECURE_RANDOM.nextInt(CODE_BOUND));
    }
}
