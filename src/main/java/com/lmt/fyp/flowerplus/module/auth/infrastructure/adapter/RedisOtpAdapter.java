package com.lmt.fyp.flowerplus.module.auth.infrastructure.adapter;

import com.lmt.fyp.flowerplus.module.auth.application.port.out.OtpStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Redis-backed OTP store.
 * Two keys per address, deliberately kept apart:
 * otp:register:{email}   Hash { hash, attempts }   TTL = code lifetime
 * otp:resend:{email}     String "1"                TTL = resend cooldown
 */
@Component
@RequiredArgsConstructor
public class RedisOtpAdapter implements OtpStorePort {

    private static final String CODE_KEY_PREFIX = "otp:register:";
    private static final String RESEND_KEY_PREFIX = "otp:resend:";
    private static final String FIELD_HASH = "hash";
    private static final String FIELD_ATTEMPTS = "attempts";

    private final StringRedisTemplate redis;

    @Override
    public void save(String email, String codeHash, Duration ttl) {
        String key = codeKey(email);

        // Delete first: this is what resets the attempt counter when a code is
        // re-issued. Writing the fields over the top of an existing hash would
        //  couleave the oldnt in place and hand the user a fresh code with its
        // guesses already spent.
        redis.delete(key);

        HashOperations<String, String, String> hash = redis.opsForHash();
        hash.putAll(key, Map.of(FIELD_HASH, codeHash, FIELD_ATTEMPTS, "0"));
        redis.expire(key, ttl);
    }

    @Override
    public Optional<String> findHash(String email) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        return Optional.ofNullable(hash.get(codeKey(email), FIELD_HASH));
    }

    @Override
    public long incrementAttempts(String email) {
        HashOperations<String, String, String> hash = redis.opsForHash();
        // HINCRBY is atomic, so two guesses arriving at once cannot both read
        // the same count and both slip under the cap. The SQL equivalent would
        // need SELECT ... FOR UPDATE to be this safe.
        //
        // Caveat: HINCRBY creates the key if it is missing. Should a code
        // expire in the gap between the caller's findHash and this call, that
        // recreates a hash holding only a counter and carrying no TTL. Harmless
        // in effect — it has no "hash" field, so findHash reports no code and
        // every verify against it is rejected, and the next save() deletes it —
        // but it is a key that will not expire on its own. Closing the window
        // properly needs a Lua script; noted rather than fixed because the
        // consequence is one stale key per abandoned registration.
        return hash.increment(codeKey(email), FIELD_ATTEMPTS, 1);
    }

    @Override
    public void invalidate(String email) {
        redis.delete(codeKey(email));
    }

    @Override
    public boolean tryAcquireResendSlot(String email, Duration interval) {
        // SET NX EX in a single round trip: writes the marker only if absent
        // and gives it the cooldown as its lifetime, so the slot reopens on its
        // own. Returns false while a previous send is still inside the window.
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(resendKey(email), "1", interval));
    }

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String resendKey(String email) {
        return RESEND_KEY_PREFIX + email;
    }
}
