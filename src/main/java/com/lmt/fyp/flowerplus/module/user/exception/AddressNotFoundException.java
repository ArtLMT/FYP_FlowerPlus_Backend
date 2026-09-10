package com.lmt.fyp.flowerplus.module.user.exception;

/** Also thrown when the address exists but belongs to someone else. */
public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String message) {
        super(message);
    }
}
