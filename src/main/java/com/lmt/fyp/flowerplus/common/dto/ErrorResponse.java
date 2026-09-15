package com.lmt.fyp.flowerplus.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lmt.fyp.flowerplus.common.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard API error structure: the same frame on every error, plus
 * {@code details} only for the codes that define one. Field meanings and
 * every error code: docs/error-codes.md.
 */
public record ErrorResponse(
        boolean success,
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) ErrorDetails details
) {

    public static ErrorResponse of(ErrorCode code, String message, String path) {
        return of(code, message, path, null);
    }

    public static ErrorResponse of(ErrorCode code, String message, String path, ErrorDetails details) {
        HttpStatus status = code.getStatus();
        return new ErrorResponse(
                false,
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                path,
                Instant.now(),
                details
        );
    }
}
