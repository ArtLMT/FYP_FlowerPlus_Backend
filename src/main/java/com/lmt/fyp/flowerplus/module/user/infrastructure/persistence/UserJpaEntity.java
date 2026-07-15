package com.lmt.fyp.flowerplus.module.user.infrastructure.persistence;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.entity.TimestampEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * PERSISTENCE ENTITY — OUTSIDE the wall (infrastructure).
 *
 * This is the JPA mapping to the user_account table. Compared to the old
 * module.user.entity.User, TWO responsibilities have been removed:
 *
 *   1. It no longer implements UserDetails. Being a Spring Security principal
 *      is an AUTHENTICATION concern, not a persistence concern — that job now
 *      belongs to {@code security.SecurityUser}.
 *
 *   2. It is no longer the object business logic operates on. That is the pure
 *      {@code domain.model.User}. This class exists only to move rows in and
 *      out of the database.
 *
 * Because UserDetails is gone, getUsername() now plainly returns the username
 * column (previously it was overridden to return the email).
 */
@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity extends TimestampEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;
}
