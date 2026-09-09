package com.lmt.fyp.flowerplus.module.auth.service;

import com.lmt.fyp.flowerplus.config.OtpProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Keyed hash for OTP codes.
 *
 * <p>An OTP is not a password: it is low-entropy (six digits), short-lived and
 * attempt-capped, so a deliberately slow hash buys nothing — and BCrypt on the
 * public /verify-email endpoint is a CPU-exhaustion vector. HMAC-SHA256 under a
 * server secret is fast, compared in constant time, and a Redis leak alone
 * cannot brute-force the code without also holding the secret.
 */
@Component
public class OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public OtpHasher(OtpProperties otpProperties) {
        this.key = new SecretKeySpec(
                otpProperties.hmacSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** Hex-encoded HMAC-SHA256 of the code. Deterministic, so verify recomputes and compares. */
    public String hash(String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            // HmacSHA256 is a required JCA algorithm; failure here is a JVM misconfiguration.
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        }
    }

    /** Constant-time comparison so a mismatch cannot be timed byte by byte. */
    public boolean matches(String code, String storedHash) {
        return MessageDigest.isEqual(
                hash(code).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
