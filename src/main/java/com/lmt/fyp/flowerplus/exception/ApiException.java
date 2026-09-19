package com.lmt.fyp.flowerplus.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import lombok.Getter;

/**
 * An error the client caused, answered during the request. The code decides the
 * status and {@code errorCode}. Subclass only when an error carries data or is
 * caught by type.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ApiException(ErrorCode code, String devMessage) {
        super(devMessage);
        this.code = code;
    }
}
