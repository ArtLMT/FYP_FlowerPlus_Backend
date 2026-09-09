package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link SecurityUser} — the Spring Security principal and the
 * single place "may this account authenticate?" is decided. Pure logic: no
 * Spring, no database.
 */
class SecurityUserTest {

    private static SecurityUser userWith(UserRole role, UserAccountStatus status) {
        return new SecurityUser("user@example.com", "hashed-password", role, status);
    }

    @Test
    @DisplayName("authorities are the single role, ROLE_-prefixed")
    void authoritiesArePrefixedRole() {
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.ACTIVE).getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");

        assertThat(userWith(UserRole.ADMIN, UserAccountStatus.ACTIVE).getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("the email is the security username")
    void usernameIsEmail() {
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.ACTIVE).getUsername())
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("only BANNED is locked")
    void lockedOnlyWhenBanned() {
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.BANNED).isAccountNonLocked()).isFalse();
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.ACTIVE).isAccountNonLocked()).isTrue();
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.PENDING).isAccountNonLocked()).isTrue();
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.SUSPENDED).isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("only PENDING is disabled")
    void disabledOnlyWhenPending() {
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.PENDING).isEnabled()).isFalse();
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.ACTIVE).isEnabled()).isTrue();
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.BANNED).isEnabled()).isTrue();
        assertThat(userWith(UserRole.CUSTOMER, UserAccountStatus.SUSPENDED).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("isAuthBlocked blocks BANNED and PENDING, allows ACTIVE and SUSPENDED")
    void authBlockedCoversBannedAndPending() {
        // The shared gate used by both the login path and the refresh path.
        assertThat(SecurityUser.isAuthBlocked(UserAccountStatus.BANNED)).isTrue();
        assertThat(SecurityUser.isAuthBlocked(UserAccountStatus.PENDING)).isTrue();
        assertThat(SecurityUser.isAuthBlocked(UserAccountStatus.ACTIVE)).isFalse();
        assertThat(SecurityUser.isAuthBlocked(UserAccountStatus.SUSPENDED)).isFalse();
    }
}
