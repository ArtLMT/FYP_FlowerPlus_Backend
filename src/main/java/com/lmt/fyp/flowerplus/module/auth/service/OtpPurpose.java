package com.lmt.fyp.flowerplus.module.auth.service;

/**
 * What an OTP authorises. It namespaces every code in the store, so a code
 * minted for one flow can never satisfy another — a password-reset code must
 * not verify a registration, and vice versa.
 *
 * <p>Only REGISTRATION exists today; PASSWORD_RESET arrives with the reset flow.
 * The dimension is introduced now so the two can never share a key later.
 */
public enum OtpPurpose {
    REGISTRATION
}
