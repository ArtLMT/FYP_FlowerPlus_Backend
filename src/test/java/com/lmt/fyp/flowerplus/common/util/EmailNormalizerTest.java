package com.lmt.fyp.flowerplus.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link EmailNormalizer}. Every email from the outside world is
 * canonicalised through here, so casing/whitespace differences can't create
 * duplicate accounts or break login matching.
 */
class EmailNormalizerTest {

    @Test
    @DisplayName("lowercases and trims surrounding whitespace")
    void lowercasesAndTrims() {
        assertThat(EmailNormalizer.normalize("  Thanh@Gmail.COM  "))
                .isEqualTo("thanh@gmail.com");
    }

    @Test
    @DisplayName("an already-canonical email is unchanged")
    void alreadyCanonicalIsUnchanged() {
        assertThat(EmailNormalizer.normalize("thanh@gmail.com"))
                .isEqualTo("thanh@gmail.com");
    }

    @Test
    @DisplayName("null in, null out")
    void nullIsPassedThrough() {
        assertThat(EmailNormalizer.normalize(null)).isNull();
    }
}
