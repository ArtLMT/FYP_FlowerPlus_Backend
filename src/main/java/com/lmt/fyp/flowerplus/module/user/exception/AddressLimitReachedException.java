package com.lmt.fyp.flowerplus.module.user.exception;

/** The customer already has the maximum number of saved addresses. */
public class AddressLimitReachedException extends RuntimeException {
    public AddressLimitReachedException(String message) {
        super(message);
    }
}
