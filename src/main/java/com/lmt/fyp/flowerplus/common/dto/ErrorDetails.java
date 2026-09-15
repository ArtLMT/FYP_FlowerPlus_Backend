package com.lmt.fyp.flowerplus.common.dto;

import java.util.List;

/**
 * The only shapes {@link ErrorResponse#details()} can take. Sealed so that
 * adding one is a deliberate change here, recorded in docs/error-codes.md.
 */
public sealed interface ErrorDetails {

    /** OTP_THROTTLED, OTP_DAILY_LIMIT_REACHED. */
    record Retry(long retryAfterSeconds) implements ErrorDetails {
    }

    /** VALIDATION_FAILED. A field failing several constraints appears once per constraint. */
    record Validation(List<FieldViolation> fields) implements ErrorDetails {
    }

    record FieldViolation(String field, String rule, String message) {
    }
}
