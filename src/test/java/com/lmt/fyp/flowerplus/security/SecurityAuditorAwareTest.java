package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: the security context is set by hand, no Spring context. */
class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a signed-in user is the auditor")
    void signedInUserIsTheAuditor() {
        UUID id = UUID.randomUUID();
        SecurityUser principal =
                new SecurityUser(id, "staff@example.com", "pw", UserRole.ADMIN, UserAccountStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(auditorAware.getCurrentAuditor()).contains(id);
    }

    @Test
    @DisplayName("no authentication means no auditor")
    void nobodySignedIn() {
        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("an anonymous request has no auditor")
    void anonymousRequest() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("any other principal type, such as the Google login user, has no auditor")
    void otherPrincipalType() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-security-user", null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
