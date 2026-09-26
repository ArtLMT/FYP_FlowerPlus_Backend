package com.lmt.fyp.flowerplus.common.util;

/**
 * Shared canonicalization for text whose leading and trailing whitespace is not meaningful.
 */
public final class StringNormalizer {

    private StringNormalizer() {
    }

    public static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
