package com.lmt.fyp.flowerplus.fake;

import com.lmt.fyp.flowerplus.module.auth.application.port.out.OtpStorePort;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOtpStore implements OtpStorePort {

    private static class OtpEntry {
        final String codeHash;
        long attempts;
        final Instant expiresAt;

        OtpEntry(String codeHash, Instant expiresAt) {
            this.codeHash = codeHash;
            this.attempts = 0;
            this.expiresAt = expiresAt;
        }

        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    private final Map<String, OtpEntry> otpMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> resendCooldownMap = new ConcurrentHashMap<>();

    @Override
    public void save(String email, String codeHash, Duration ttl) {
        otpMap.put(email, new OtpEntry(codeHash, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> findHash(String email) {
        OtpEntry entry = otpMap.get(email);
        if (entry == null || entry.isExpired(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.codeHash);
    }

    @Override
    public long incrementAttempts(String email) {
        OtpEntry entry = otpMap.get(email);
        if (entry == null) {
            return 1;
        }
        return ++entry.attempts;
    }

    @Override
    public void invalidate(String email) {
        otpMap.remove(email);
    }

    @Override
    public boolean tryAcquireResendSlot(String email, Duration interval) {
        Instant now = Instant.now();
        Instant availableAt = resendCooldownMap.get(email);
        if (availableAt != null && now.isBefore(availableAt)) {
            return false;
        }
        resendCooldownMap.put(email, now.plus(interval));
        return true;
    }

    /**
     * Test-only accessor returning the plaintext code / hash stored for a given email key.
     */
    public Optional<String> getPlaintextCode(String email) {
        return findHash(email);
    }

    /**
     * Test-only accessor returning the stored code for a given email key.
     */
    public Optional<String> getCode(String email) {
        return findHash(email);
    }

    public void clear() {
        otpMap.clear();
        resendCooldownMap.clear();
    }
}
