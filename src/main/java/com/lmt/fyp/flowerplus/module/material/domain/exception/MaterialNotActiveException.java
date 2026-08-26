package com.lmt.fyp.flowerplus.module.material.domain.exception;

/**
 * DOMAIN EXCEPTION — INSIDE the wall.
 *
 * Thrown when a material that is not ACTIVE is used where BR-MAT-06 demands an
 * active one: added to a recipe, or received as a new batch.
 *
 * Deliberately a plain RuntimeException — see InvalidMaterialException for why
 * the core does not reference ErrorCode or Spring's HttpStatus. The web layer
 * (GlobalExceptionHandler) maps this to a 409 response.
 */
public class MaterialNotActiveException extends RuntimeException {
    public MaterialNotActiveException(String message) {
        super(message);
    }
}
