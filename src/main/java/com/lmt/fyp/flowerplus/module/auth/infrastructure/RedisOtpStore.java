package com.lmt.fyp.flowerplus.module.auth.infrastructure;

import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;
import com.lmt.fyp.flowerplus.module.auth.service.OtpStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Redis-backed OTP store.
 * Two keys per (purpose, address), deliberately kept apart:
 * otp:{purpose}:{email}          Hash { hash, attempts }   TTL = code lifetime
 * otp:resend:{purpose}:{email}   String "1"                TTL = resend cooldown
 *
 * <p>The purpose namespaces the key so a code issued for one flow (say a
 * password reset) can never satisfy another (a registration).
 */
@Component
@RequiredArgsConstructor
public class RedisOtpStore implements OtpStore {

    private static final String CODE_KEY_PREFIX = "otp:";
    private static final String RESEND_KEY_PREFIX = "otp:resend:";
    private static final String FIELD_HASH = "hash";
    private static final String FIELD_ATTEMPTS = "attempts";

    private final StringRedisTemplate redis;

    @Override
    public void save(OtpPurpose purpose, String email, String codeHash, Duration ttl) {
        String key = codeKey(purpose, email);

        // Delete first: this is what resets the attempt counter when a code is
        // re-issued. Writing the fields over the top of an existing hash would
        // leave the old count in place and hand the user a fresh code with its
        // guesses already spent.
        redis.delete(key);

        HashOperations<String, String, String> hash = redis.opsForHash();
        hash.putAll(key, Map.of(FIELD_HASH, codeHash, FIELD_ATTEMPTS, "0"));
        redis.expire(key, ttl);
    }

    @Override
    public Optional<String> findHash(OtpPurpose purpose, String email) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        return Optional.ofNullable(hash.get(codeKey(purpose, email), FIELD_HASH));
    }

    @Override
    public long incrementAttempts(OtpPurpose purpose, String email) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        // HINCRBY is atomic, so two guesses arriving at once cannot both read
        // the same count and both slip under the cap.
        //
        // Caveat: HINCRBY creates the key if it is missing. Should a code
        // expire between the caller findHash and this call, that recreates a
        // hash holding only a counter and carrying no TTL. Harmless in effect
        // (no "hash" field means findHash reports no code, and the next save
        // deletes it) but it is a key that will not expire on its own. Closing
        // the window properly needs a Lua script; noted rather than fixed
        // because the cost is one stale key per abandoned registration.
        return hash.increment(codeKey(purpose, email), FIELD_ATTEMPTS, 1);
    }

    @Override
    public void invalidate(OtpPurpose purpose, String email) {
        redis.delete(codeKey(purpose, email));
    }

    @Override
    public boolean tryAcquireResendSlot(OtpPurpose purpose, String email, Duration interval) {
        // SET NX EX in a single round trip: writes the marker only if absent
        // and gives it the cooldown as its lifetime, so the slot reopens on its
        // own. Returns false while a previous send is still inside the window.
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(resendKey(purpose, email), "1", interval));
    }

    private String codeKey(OtpPurpose purpose, String email) {
        return CODE_KEY_PREFIX + slug(purpose) + ":" + email;
    }

    private String resendKey(OtpPurpose purpose, String email) {
        return RESEND_KEY_PREFIX + slug(purpose) + ":" + email;
    }

    private String slug(OtpPurpose purpose) {
        return purpose.name().toLowerCase(Locale.ROOT);
    }
}
