package com.lmt.fyp.flowerplus.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test of the role hierarchy bean, no Spring context. */
class RoleHierarchyTest {

    private final RoleHierarchy hierarchy = SecurityConfig.roleHierarchy();

    private Collection<String> reachableFrom(String role) {
        return hierarchy.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList(role))
                .stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    @DisplayName("ADMIN reaches STAFF")
    void adminReachesStaff() {
        assertThat(reachableFrom("ROLE_ADMIN")).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STAFF");
    }

    @Test
    @DisplayName("STAFF does not reach ADMIN")
    void staffDoesNotReachAdmin() {
        assertThat(reachableFrom("ROLE_STAFF")).containsExactly("ROLE_STAFF");
    }

    @Test
    @DisplayName("CUSTOMER gains nothing")
    void customerGainsNothing() {
        assertThat(reachableFrom("ROLE_CUSTOMER")).containsExactly("ROLE_CUSTOMER");
    }
}
