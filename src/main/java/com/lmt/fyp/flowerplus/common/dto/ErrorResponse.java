package com.lmt.fyp.flowerplus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error structure.
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
    private Map<String, String> validationErrors;
}
