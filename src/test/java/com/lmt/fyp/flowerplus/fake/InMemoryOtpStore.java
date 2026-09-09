package com.lmt.fyp.flowerplus.fake;

import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;
import com.lmt.fyp.flowerplus.module.auth.service.OtpStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOtpStore implements OtpStore {

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
    public void save(OtpPurpose purpose, String email, String codeHash, Duration ttl) {
        otpMap.put(key(purpose, email), new OtpEntry(codeHash, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> findHash(OtpPurpose purpose, String email) {
        OtpEntry entry = otpMap.get(key(purpose, email));
        if (entry == null || entry.isExpired(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.codeHash);
    }

    @Override
    public long incrementAttempts(OtpPurpose purpose, String email) {
        OtpEntry entry = otpMap.get(key(purpose, email));
        if (entry == null) {
            return 1;
        }
        return ++entry.attempts;
    }

    @Override
    public void invalidate(OtpPurpose purpose, String email) {
        otpMap.remove(key(purpose, email));
    }

    @Override
    public boolean tryAcquireResendSlot(OtpPurpose purpose, String email, Duration interval) {
        String key = key(purpose, email);
        Instant now = Instant.now();
        Instant availableAt = resendCooldownMap.get(key);
        if (availableAt != null && now.isBefore(availableAt)) {
            return false;
        }
        resendCooldownMap.put(key, now.plus(interval));
        return true;
    }

    public void clear() {
        otpMap.clear();
        resendCooldownMap.clear();
    }

    private static String key(OtpPurpose purpose, String email) {
        return purpose + "|" + email;
    }
}
