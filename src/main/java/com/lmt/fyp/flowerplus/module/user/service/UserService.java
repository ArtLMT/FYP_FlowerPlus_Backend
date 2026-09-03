package com.lmt.fyp.flowerplus.module.user.service;

import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * User account operations published to the rest of the application, including
 * the account lifecycle that auth drives. This module owns that lifecycle;
 * auth supplies an already-hashed password and never touches the repositories.
 */
public interface UserService {

    /**
     * @throws com.lmt.fyp.flowerplus.module.user.exception.UserNotFoundException
     *         if no account has this id
     */
    User getUserById(UUID id);

    /**
     * The account for this email.
     *
     * @throws com.lmt.fyp.flowerplus.module.user.exception.UserNotFoundException
     *         if no account has this email
     */
    User getByEmail(String email);

    /** The user's profile, or {@code null} if none has been created yet. */
    UserProfile getProfile(User user);

    /** Optional variant for callers that branch on absence; see {@link #getByEmail}. */
    Optional<User> findByEmail(String email);

    /** Creates a PENDING account plus its profile. Email doubles as the username. */
    User createPendingAccount(String email, String hashedPassword, String fullName);

    /** Re-registration onto a still-PENDING account: new password, new profile name. */
    void resetPendingAccount(User user, String hashedPassword, String fullName);

    /** Marks a verified account ACTIVE. Relies on dirty checking inside the caller's transaction. */
    void activate(User user);
}
