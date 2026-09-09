package com.lmt.fyp.flowerplus.module.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for profile-read authorization (items 2, 3, 18): the
 * owner-or-ADMIN guard on GET /api/users/{id}, the /me shortcut, and the
 * unauthenticated case. The cross-user 403 is the one test that fails if
 * method security (@EnableMethodSecurity) is ever switched off.
 */
class UserAccessTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    /** Create two ACTIVE users and return A's access token plus both users. */
    private String accessTokenFor(String email) throws Exception {
        createUser(email, PASSWORD, UserAccountStatus.ACTIVE);
        JsonNode tokens = loginTokens(email, PASSWORD);
        return tokens.get("flowerplus_at").asText();
    }

    @Test
    @DisplayName("a user can read their own profile by id")
    void ownerReadsOwnProfile() throws Exception {
        User a = createUser("owner@example.com", PASSWORD, UserAccountStatus.ACTIVE);
        String tokenA = loginTokens("owner@example.com", PASSWORD).get("flowerplus_at").asText();

        mockMvc.perform(get("/api/users/" + a.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()));
    }

    @Test
    @DisplayName("a user cannot read another user's profile")
    void crossUserReadIsForbidden() throws Exception {
        String tokenA = accessTokenFor("a@example.com");
        User b = createUser("b@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        mockMvc.perform(get("/api/users/" + b.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("/me returns the caller's own profile")
    void meReturnsSelf() throws Exception {
        User a = createUser("me@example.com", PASSWORD, UserAccountStatus.ACTIVE);
        String tokenA = loginTokens("me@example.com", PASSWORD).get("flowerplus_at").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()));
    }

    @Test
    @DisplayName("no token is unauthenticated")
    void noTokenIsUnauthenticated() throws Exception {
        User a = createUser("anon@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        mockMvc.perform(get("/api/users/" + a.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
