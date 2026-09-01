package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * SPRING SECURITY PRINCIPAL — a security-layer adapter.
 *
 * Holds the UserDetails responsibility so the persistence entity stays a plain
 * data object. Spring Security depends on THIS; the rest of the app depends on
 * the entity.
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
    public static SecurityUser fromEntity(User user) {
        return new SecurityUser(user.getEmail(), user.getPassword(), user.getRole(), user.getStatus());
    }

    /**
     * Single source of truth for "may this account authenticate at all?".
     * BANNED and PENDING are blocked; SUSPENDED users can still sign in (they
     * are restricted at the order layer, not here). Shared with RefreshService
     * so the refresh path stays aligned with the login path.
     *
     * <p>The two UserDetails flags below split this same set on purpose so the
     * login path can give a different message for each: BANNED surfaces as a
     * {@code LockedException}, PENDING as a {@code DisabledException}. Their
     * union must stay equal to this method.
     */
    public static boolean isAuthBlocked(UserAccountStatus status) {
        return status == UserAccountStatus.BANNED || status == UserAccountStatus.PENDING;
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

    /**
     * "Locked" means BANNED. Spring checks this before {@link #isEnabled()},
     * so a banned account fails here with a LockedException before the
     * disabled check is ever reached.
     */
    @Override
    public boolean isAccountNonLocked() {
        return status != UserAccountStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * "Disabled" means PENDING (email not yet verified). A banned account never
     * reaches this check — it is already rejected as locked — so PENDING is the
     * only status that produces a DisabledException, which the exception handler
     * maps to a "verify your email" message rather than "account blocked".
     */
    @Override
    public boolean isEnabled() {
        return status != UserAccountStatus.PENDING;
    }
}
