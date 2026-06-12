package com.lmt.fyp.flowerplus.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;

/**
 * Thrown when a request carries invalid parameters or fails logic checks.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(ErrorCode code) {
        super(code);
    }

    public BadRequestException(ErrorCode code, String devMessage) {
        super(code, devMessage);
    }
}
