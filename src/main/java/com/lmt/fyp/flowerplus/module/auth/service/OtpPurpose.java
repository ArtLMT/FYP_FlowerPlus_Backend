package com.lmt.fyp.flowerplus.module.auth.service;

/**
 * What an OTP authorises. It namespaces every code in the store, so a code
 * minted for one flow can never satisfy another — a password-reset code must
 * not verify a registration, and vice versa.
 */
public enum OtpPurpose {
    REGISTRATION,
    PASSWORD_RESET
}
