package com.lmt.fyp.flowerplus.module.user.event;

/**
 * Published by the user module once an Admin has created a Staff
 * account and that write has committed. The user module knows nothing about how
 * a first password is delivered; auth listens for this and issues the code.
 * Carries the normalized email — the only thing the listener needs.
 */
public record StaffAccountCreatedEvent(String email) {
}
