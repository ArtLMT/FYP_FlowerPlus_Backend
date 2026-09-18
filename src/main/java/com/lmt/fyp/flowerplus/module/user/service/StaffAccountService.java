package com.lmt.fyp.flowerplus.module.user.service;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * The Admin's Staff-account surface. Creating, deactivating and listing Staff is
 * an Admin-only operation (BR-AUTH-05); all of it targets STAFF accounts only.
 * The one Admin account is never created or changed here — it comes from
 * {@code AdminAccountInitializer} at startup.
 */
public interface StaffAccountService {

    /**
     * Creates a Staff account for this email and starts its first-password flow.
     * A brand-new email becomes an ACTIVE STAFF account with a random password;
     * a PENDING (never-verified) account on that email is adopted as STAFF. The
     * caller-supplied name becomes the account's profile name.
     *
     * @throws com.lmt.fyp.flowerplus.exception.ApiException
     *         {@code EMAIL_ALREADY_EXISTS} if an ACTIVE, SUSPENDED or BANNED
     *         account already owns the email
     */
    User createStaff(String email, String fullName);

    /**
     * Deactivates a Staff account (ACTIVE → BANNED). deactivating an already-deactivated account changes nothing and returns it.
     *
     * @throws com.lmt.fyp.flowerplus.exception.ApiException
     *         {@code USER_NOT_FOUND} if no STAFF account has this id
     */
    User deactivate(UUID id);

    /**
     * Reactivates a Staff account (BANNED → ACTIVE). Idempotent: reactivating an already-active account changes nothing and returns it.
     *
     * @throws com.lmt.fyp.flowerplus.exception.ApiException
     *         {@code USER_NOT_FOUND} if no STAFF account has this id
     */
    User reactivate(UUID id);

    /** A page of Staff accounts; {@code status} narrows to that status when non-null. */
    Page<User> listStaff(UserAccountStatus status, Pageable pageable);
}
