package com.lmt.fyp.flowerplus.module.auth.application.exception;

/**
 * The submitted code did not match, or no code is outstanding for the email.
 *
 * <p>Deliberately covers three cases that the caller must not be able to tell
 * apart: wrong code, expired code, and no code ever issued. Redis drops the key
 * on expiry, so "expired" and "never existed" are already indistinguishable to
 * us; collapsing "wrong" into the same response keeps the endpoint from
 * confirming which addresses are registered.
 */
public class OtpInvalidException extends RuntimeException {
    public OtpInvalidException(String message) {
        super(message);
    }
}
