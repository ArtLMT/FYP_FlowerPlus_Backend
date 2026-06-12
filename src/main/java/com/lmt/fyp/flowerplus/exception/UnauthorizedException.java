package com.lmt.fyp.flowerplus.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;

/**
 * Thrown when credentials fail or are missing.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(ErrorCode code) {
        super(code);
    }

    public UnauthorizedException(ErrorCode code, String devMessage) {
        super(code, devMessage);
    }
}
