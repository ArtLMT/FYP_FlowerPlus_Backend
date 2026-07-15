package com.lmt.fyp.flowerplus.module.user.domain.model;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * DOMAIN MODEL — the heart of the module, INSIDE the wall.
 *
 * Notice what is NOT here compared to the JPA entity in
 * {@code module.user.entity.User}:
 *   - no @Entity / @Table / @Column        (no JPA/persistence knowledge)
 *   - no implements UserDetails            (no Spring Security knowledge)
 *
 * It only imports plain Java + your own pure enums. This is the object your
 * business rules operate on. Persistence and security are OUTSIDE concerns
 * that the adapters translate to/from this shape.
 */
@Getter
@Builder
public class User {

    private final UUID id;
    private final String username;
    private final String email;
    private final UserRole role;
    private final UserAccountStatus status;
    private final AuthProvider provider;
    private final UserProfile profile;

    /** A business rule expressed in the domain, not scattered in controllers. */
    public boolean isActive() {
        return status == UserAccountStatus.ACTIVE;
    }

    /** Prefer the profile's full name; fall back to the account username. */
    public String displayName() {
        return (profile != null && profile.getFullName() != null)
                ? profile.getFullName()
                : username;
    }
}
