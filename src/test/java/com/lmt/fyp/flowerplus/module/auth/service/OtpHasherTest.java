package com.lmt.fyp.flowerplus.module.auth.service;

import com.lmt.fyp.flowerplus.config.OtpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link OtpHasher} — plain Java, no Spring context and no
 * database, so it runs in milliseconds. It builds the hasher by hand with a
 * throwaway secret and checks the four properties an OTP hash must have.
 */
class OtpHasherTest {

    // The "subject under test" — the object each test exercises.
    private OtpHasher hasher;

    // @BeforeEach runs before every @Test, giving each a fresh, independent hasher.
    @BeforeEach
    void setUp() {
        // Arrange (shared): OtpProperties is a record; only the secret matters here,
        // the other fields are filler because OtpHasher only reads hmacSecret().
        OtpProperties properties = new OtpProperties(
                Duration.ofMinutes(5), 5, Duration.ofSeconds(60), "test-only-secret");
        hasher = new OtpHasher(properties);
    }

    @Test
    @DisplayName("hashing the same code twice yields the same hash")
    void hashIsDeterministic() {
        // Act
        String first = hasher.hash("123456");
        String second = hasher.hash("123456");

        // Assert
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("the correct code matches its own hash")
    void matchesAcceptsTheCorrectCode() {
        String stored = hasher.hash("123456");

        assertThat(hasher.matches("123456", stored)).isTrue();
    }

    @Test
    @DisplayName("a wrong code does not match")
    void matchesRejectsTheWrongCode() {
        String stored = hasher.hash("123456");

        assertThat(hasher.matches("000000", stored)).isFalse();
    }

    @Test
    @DisplayName("different codes hash to different values")
    void distinctCodesHashDifferently() {
        assertThat(hasher.hash("111111")).isNotEqualTo(hasher.hash("222222"));
    }
}
