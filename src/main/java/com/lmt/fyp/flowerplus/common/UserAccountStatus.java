package com.lmt.fyp.flowerplus.common;

public enum UserAccountStatus {
    ACTIVE,
    SUSPENDED,
    PENDING,
    BANNED;

    /** SUSPENDED may still sign in; it is restricted at ordering instead. */
    public boolean canAuthenticate() {
        return this != BANNED && this != PENDING;
    }

    public boolean canPlaceOrder() {
        return this == ACTIVE;
    }
}
