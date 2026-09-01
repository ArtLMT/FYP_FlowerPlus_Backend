package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.config.OtpProperties;
import com.lmt.fyp.flowerplus.module.auth.event.OtpRequestedEvent;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpAttemptsExceededException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpInvalidException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpThrottledException;
import com.lmt.fyp.flowerplus.module.auth.service.OtpService;
import com.lmt.fyp.flowerplus.module.auth.service.OtpStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_ORIGIN = 100_000;
    private static final int CODE_BOUND = 900_000;

    private final OtpStore otpStore;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final OtpProperties otpProperties;

    @Override
    public void issueOTP(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (!otpStore.tryAcquireResendSlot(normalizedEmail, otpProperties.resendInterval())) {
            throw new OtpThrottledException("A code was sent recently. Please wait before requesting another.");
        }

        String code = generateOTP();

        otpStore.save(normalizedEmail, passwordEncoder.encode(code), otpProperties.ttl());

        eventPublisher.publishEvent(new OtpRequestedEvent(normalizedEmail, code));
    }

    @Override
    public void verify(String email, String code) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        String storedHash = otpStore.findHash(normalizedEmail)
                .orElseThrow(() -> new OtpInvalidException("Verification code is incorrect or has expired."));

        long used = otpStore.incrementAttempts(normalizedEmail);

        if (used > otpProperties.maxAttempts()) {
            otpStore.invalidate(normalizedEmail);
            throw new OtpAttemptsExceededException("Too many incorrect attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(code, storedHash)) {
            throw new OtpInvalidException("Verification code is incorrect or has expired.");
        }

        otpStore.invalidate(normalizedEmail);
    }

    private String generateOTP() {
        return String.valueOf(CODE_ORIGIN + SECURE_RANDOM.nextInt(CODE_BOUND));
    }
}
