package com.lmt.fyp.flowerplus.module.user.exception;

/**
 * Thrown when a user cannot be found. Framework-free: the web layer
 * (GlobalExceptionHandler) maps it to a 404 response.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
