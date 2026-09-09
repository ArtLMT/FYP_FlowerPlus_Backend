package com.lmt.fyp.flowerplus.module.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for refresh-token rotation and reuse detection (item 11).
 *
 * <p>Tokens are sent in the JSON body, never a cookie — the endpoint prefers a
 * cookie over the body, so a cookie would silently carry the rotated token
 * forward and we'd never be replaying the old one.
 */
class RefreshRotationTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    /** POST /api/auth/refresh with the token in the body. */
    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("flowerplus_rt", refreshToken))));
    }

    /** Create an ACTIVE user and log in, returning a live refresh token. */
    private String loginAndGetRefreshToken(String email) throws Exception {
        createUser(email, PASSWORD, UserAccountStatus.ACTIVE);
        JsonNode tokens = loginTokens(email, PASSWORD);
        return tokens.get("flowerplus_rt").asText();
    }

    @Test
    @DisplayName("a refresh returns a brand-new refresh token")
    void refreshRotatesTheToken() throws Exception {
        String rt1 = loginAndGetRefreshToken("rotate@example.com");

        String rt2 = objectMapper.readTree(
                        refresh(rt1).andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString())
                .get("flowerplus_rt").asText();

        assertThat(rt2).isNotEqualTo(rt1);
    }

    @Test
    @DisplayName("replaying an already-rotated token is rejected")
    void replayingARotatedTokenIsRejected() throws Exception {
        String rt1 = loginAndGetRefreshToken("replay@example.com");

        refresh(rt1).andExpect(status().isOk());   // rotates rt1 away

        refresh(rt1)                                 // rt1 is now a spent tombstone
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("reuse of a spent token wipes the whole family")
    void reuseWipesTheFamily() throws Exception {
        String rt1 = loginAndGetRefreshToken("family@example.com");

        String rt2 = objectMapper.readTree(
                        refresh(rt1).andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString())
                .get("flowerplus_rt").asText();

        // Replay the spent rt1 -> reuse detected -> every token for this user dropped.
        refresh(rt1).andExpect(status().isUnauthorized());

        // rt2 was the live token, but the family wipe killed it too.
        refresh(rt2)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("an unknown token is rejected")
    void unknownTokenIsRejected() throws Exception {
        refresh("this-token-was-never-issued")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));
    }
}
