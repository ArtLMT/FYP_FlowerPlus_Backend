package com.lmt.fyp.flowerplus.module.email.exception;

/**
 * Thrown when a message could not be handed to the mail server. Delivery runs
 * asynchronously off an event, so this is logged by the listener rather than
 * surfaced to the caller.
 */
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
