package com.lmt.fyp.flowerplus.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Central registry of string length limits, so one number is not repeated
 * across a DTO annotation, a domain invariant and a column definition.
 *
 * <p>Each constant carries:
 * <ul>
 *   <li>{@code min} — shortest accepted length, after trimming.</li>
 *   <li>{@code max} — longest accepted length, matching the column width in
 *       {@code db/migration}. SQL cannot read these values, so a migration
 *       that changes a column width must change the constant too.</li>
 * </ul>
 *
 * <p><b>Not usable in {@code @Size(max = ...)}.</b> Annotation elements must be
 * compile-time constant expressions, and {@code LIMIT.getMax()} is a method
 * call. Use these in domain invariants and service code; for Bean Validation,
 * pass the constant itself to a custom constraint annotation.
 */
@Getter
@RequiredArgsConstructor
public enum ValidationLimit {

    /* ===================== MATERIAL ===================== */
    MATERIAL_NAME(1, 255),

    /* ===================== USER ========================= */
    USER_USERNAME(1, 255),
    USER_EMAIL(3, 255),
    USER_FULL_NAME(2, 255),
    /** BCrypt ignores input beyond 72 bytes, so a longer password is misleading. */
    USER_PASSWORD(8, 72),
    USER_PHONE(8, 20);

    private final int min;
    private final int max;

    /**
     * True when {@code value} fits this limit once trimmed.
     * Null and blank are never valid — every limit here has a min of at least 1.
     */
    public boolean accepts(String value) {
        if (value == null) {
            return false;
        }
        int length = value.trim().length();
        return length >= min && length <= max;
    }
}
