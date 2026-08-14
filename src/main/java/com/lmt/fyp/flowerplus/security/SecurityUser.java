package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * SPRING SECURITY PRINCIPAL — a security-layer adapter.
 *
 * This class holds the UserDetails responsibility that used to live ON the
 * User entity. Keeping it here means:
 *   - the persistence entity stays a plain data object, and
 *   - the domain model never learns what "authorities" or "credentials
 *     non-expired" mean.
 *
 * Spring Security depends on THIS; the rest of the app depends on the domain
 * model. That is the separation the refactor is about.
 */
public class SecurityUser implements UserDetails {

    private final String email;
    private final String password;
    private final UserRole role;
    private final UserAccountStatus status;

    public SecurityUser(String email, String password, UserRole role, UserAccountStatus status) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    /** Build a security principal from a persistence entity. */
    public static SecurityUser fromEntity(UserJpaEntity user) {
        return new SecurityUser(user.getEmail(), user.getPassword(), user.getRole(), user.getStatus());
    }

    /**
     * Single source of truth for "may this account authenticate at all?".
     * Only BANNED is blocked; SUSPENDED users can still sign in (they are
     * restricted at the order layer, not here). Shared with RefreshService so
     * the refresh path and the UserDetails flags never drift apart.
     */
    public static boolean isAuthBlocked(UserAccountStatus status) {
        return status == UserAccountStatus.BANNED;
    }

    /** The account status, for authorization checks beyond authentication. */
    public UserAccountStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** Email is the security "username" (the JWT subject / login principal). */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isAuthBlocked(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isAuthBlocked(status);
    }
}
