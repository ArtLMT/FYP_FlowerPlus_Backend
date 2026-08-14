package com.lmt.fyp.flowerplus.common.util;

import java.util.Locale;

/**
 * Canonicalizes email addresses to a single stored form: trimmed and
 * lowercased. Postgres UNIQUE is case-sensitive, so without this
 * {@code Thanh@Gmail.com} and {@code thanh@gmail.com} would become two
 * separate accounts, and a login whose casing differs from registration
 * would fail to match.
 *
 * <p>Every entry point that reads an email from the outside world (register,
 * login, OAuth2 provider attributes) must run it through here.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    /**
     * @return the email trimmed and lowercased using {@link Locale#ROOT},
     *         or {@code null} if the input is {@code null}.
     *         Locale.ROOT is deliberate: default-locale lowercasing maps
     *         {@code I} to {@code ı} under a Turkish locale, silently
     *         corrupting addresses.
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
