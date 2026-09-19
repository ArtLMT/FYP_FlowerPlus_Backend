package com.lmt.fyp.flowerplus.module.auth.exception;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.ApiException;

/**
 * A refresh token refused inside {@code RefreshTokenService.rotate}.
 *
 * <p>Its own type only because rotate()'s {@code noRollbackFor} names it: on
 * reuse the token family is deleted and then this is thrown, and that delete
 * must still commit. Any other refresh refusal is a plain {@link ApiException}.
 */
public class RefreshTokenRejectedException extends ApiException {

    public RefreshTokenRejectedException(ErrorCode code) {
        super(code);
    }
}
