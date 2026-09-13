package com.lmt.fyp.flowerplus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error structure. Field meanings and every error code:
 * docs/error-codes.md.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private int status;
    private String errorCode;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
    private Long retryAfterSeconds;
    private Map<String, String> validationErrors;
    private Map<String, String> validationRules;
}
