package com.lmt.fyp.flowerplus.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;

/**
 * Thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode code) {
        super(code);
    }

    public ResourceNotFoundException(ErrorCode code, String devMessage) {
        super(code, devMessage);
    }
}
