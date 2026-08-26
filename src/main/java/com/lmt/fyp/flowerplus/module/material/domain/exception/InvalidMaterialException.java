package com.lmt.fyp.flowerplus.module.material.domain.exception;

/**
 * DOMAIN EXCEPTION — INSIDE the wall.
 *
 * Thrown by Material's factory and rename() when an invariant is violated:
 * a missing name, type or unit; a name over 255 characters; or a shelf life
 * that contradicts the perishable flag (BR-MAT-03, BR-MAT-10).
 *
 * Deliberately a plain RuntimeException: it does NOT reference ErrorCode or
 * Spring's HttpStatus, because the core must not know about HTTP. The web
 * layer (GlobalExceptionHandler) is what maps this to a 400 response.
 */
public class InvalidMaterialException extends RuntimeException {
    public InvalidMaterialException(String message) {
        super(message);
    }
}
