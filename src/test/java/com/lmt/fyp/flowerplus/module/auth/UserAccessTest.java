package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profile-read authorization after lookup-by-id became Admin-only (accounts P2):
 * a user reads themselves through GET /api/users/me, and looking up any account
 * by id lives at GET /api/admin/users/{id}, guarded by the /api/admin/** rule
 * and @PreAuthorize. The old GET /api/users/{id} is gone. The Staff 403 is the
 * test that fails if method security (@EnableMethodSecurity) is ever switched
 * off — the URL rule alone would still let an Admin through, but not prove the
 * @PreAuthorize layer.
 */
class UserAccessTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    private String accessTokenFor(String email, UserRole role) throws Exception {
        createUser(email, PASSWORD, UserAccountStatus.ACTIVE, role);
        return loginTokens(email, PASSWORD).access();
    }

    @Test
    @DisplayName("/me returns the caller's own profile")
    void meReturnsSelf() throws Exception {
        User a = createUser("me@example.com", PASSWORD, UserAccountStatus.ACTIVE);
        String tokenA = loginTokens("me@example.com", PASSWORD).access();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()));
    }

    @Test
    @DisplayName("the old GET /api/users/{id} no longer exists")
    void oldLookupPathIsGone() throws Exception {
        String tokenA = accessTokenFor("seeker@example.com", UserRole.CUSTOMER);

        mockMvc.perform(get("/api/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("an Admin can look up any account by id")
    void adminReadsAnyUser() throws Exception {
        String adminToken = accessTokenFor("admin@example.com", UserRole.ADMIN);
        User target = createUser("target@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        mockMvc.perform(get("/api/admin/users/" + target.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId().toString()))
                .andExpect(jsonPath("$.email").value("target@example.com"));
    }

    @Test
    @DisplayName("a Staff member cannot use the admin lookup")
    void staffCannotUseAdminLookup() throws Exception {
        String staffToken = accessTokenFor("staff@example.com", UserRole.STAFF);
        User target = createUser("target@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        mockMvc.perform(get("/api/admin/users/" + target.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("an id with no user is USER_NOT_FOUND for the Admin")
    void unknownIdIsNotFound() throws Exception {
        String adminToken = accessTokenFor("admin@example.com", UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("no token is unauthenticated")
    void noTokenIsUnauthenticated() throws Exception {
        User a = createUser("anon@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        mockMvc.perform(get("/api/admin/users/" + a.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
