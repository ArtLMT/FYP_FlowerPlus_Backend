package com.lmt.fyp.flowerplus.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized registry of all business error codes.
 *
 * <p>Each constant carries:
 * <ul>
 *   <li>{@code status}     — the HTTP status code to return to the client.</li>
 *   <li>{@code messageKey} — the key used to look up the i18n message in
 *       {@code messages.properties} (and locale variants).</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* ===================== AUTH ===================== */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "auth.invalidCredentials"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "auth.unauthenticated"),
    ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "auth.accountBlocked"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "auth.refreshTokenExpired"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "auth.refreshTokenInvalid"),

    /* ===================== USER ===================== */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user.notFound"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "user.emailAlreadyExists"),

    /* ===================== VALIDATION ===================== */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "validation.failed"),

    /* ===================== SECURITY ===================== */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "security.accessDenied"),

    /* ===================== SYSTEM ===================== */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "system.internalError");

    private final HttpStatus status;
    private final String messageKey;
}
