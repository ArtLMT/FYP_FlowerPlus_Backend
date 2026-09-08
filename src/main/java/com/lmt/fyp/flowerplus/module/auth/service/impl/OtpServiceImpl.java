package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.config.OtpProperties;
import com.lmt.fyp.flowerplus.module.auth.event.EmailVerifiedEvent;
import com.lmt.fyp.flowerplus.module.auth.event.OtpRequestedEvent;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpAttemptsExceededException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpInvalidException;
import com.lmt.fyp.flowerplus.module.auth.exception.OtpThrottledException;
import com.lmt.fyp.flowerplus.module.auth.service.OtpService;
import com.lmt.fyp.flowerplus.module.auth.service.OtpStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.security.SecureRandom;

@Slf4j
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

        // Success does NOT consume the code here. EmailVerificationService
        // activates the account, then publishes EmailVerifiedEvent, and
        // onEmailVerified() below invalidates the code AFTER_COMMIT — so a code
        // is spent only once activation is durable. The invalidate above (on the
        // attempt cap) stays immediate: that is a lockout, not a consume.
    }

    /**
     * Invalidates a code once its verification transaction has committed.
     * Runs AFTER_COMMIT and is intentionally NOT @Async: the account is already
     * active, so a failure here is harmless — the stale code lives out its short
     * TTL — and must never surface as a request error.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerified(EmailVerifiedEvent event) {
        try {
            otpStore.invalidate(event.email());
        } catch (Exception e) {
            // Failure only happen if otpStore(redis) infra is down,
            // without catch it'll return by default on controller,
            // resulting in 500 when the account is already Active.
            // This log mean verification worked, but the cache can't be delete and will be after it TTL
            log.warn("Failed to invalidate OTP after verification for {}", event.email(), e);
        }
    }

    private String generateOTP() {
        return String.valueOf(CODE_ORIGIN + SECURE_RANDOM.nextInt(CODE_BOUND));
    }
}
