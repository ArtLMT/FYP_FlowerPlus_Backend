package com.lmt.fyp.flowerplus.module.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    // No setter: the lifecycle moves only through activate().
    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    public void activate() {
        if (status != UserAccountStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING account can be activated, was " + status);
        }
        status = UserAccountStatus.ACTIVE;
    }

    /**
     * Take over an unverified (PENDING) account as a newly created Staff member:
     * activate it, make it STAFF, and replace its never-proven password with a
     * fresh hash the Admin never sees. Only a PENDING account can be adopted —
     * an ACTIVE, SUSPENDED or BANNED email already belongs to someone.
     */
    public void adoptAsStaff(String hashedPassword) {
        if (status != UserAccountStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING account can be adopted, was " + status);
        }
        status = UserAccountStatus.ACTIVE;
        role = UserRole.STAFF;
        password = hashedPassword;
    }

    /** Deactivate a Staff account: ACTIVE → BANNED. Reversible via {@link #unban()}. */
    public void ban() {
        if (status != UserAccountStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE account can be banned, was " + status);
        }
        status = UserAccountStatus.BANNED;
    }

    /** Reactivate a Staff account: BANNED → ACTIVE. */
    public void unban() {
        if (status != UserAccountStatus.BANNED) {
            throw new IllegalStateException("Only a BANNED account can be unbanned, was " + status);
        }
        status = UserAccountStatus.ACTIVE;
    }
}
