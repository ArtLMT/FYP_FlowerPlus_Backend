package com.lmt.fyp.flowerplus.module.user.application.exception;

/**
 * APPLICATION EXCEPTION — INSIDE the wall.
 *
 * Deliberately a plain RuntimeException: it does NOT reference ErrorCode or
 * Spring's HttpStatus, because the core must not know about HTTP. The web
 * layer (GlobalExceptionHandler) is what maps this to a 404 response.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
