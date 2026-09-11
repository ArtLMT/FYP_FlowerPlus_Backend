package com.lmt.fyp.flowerplus.common;

/**
 * The one password policy, applied wherever a password is set: registration,
 * password reset and the admin account. The maximum is bcrypt's — it only reads
 * the first 72 bytes, so anything longer would be silently truncated.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static boolean isSatisfiedBy(String password) {
        return password != null
                && password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH;
    }
}
