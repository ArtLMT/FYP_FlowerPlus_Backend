package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import jakarta.servlet.http.Cookie;
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
 * <p>Each call sends exactly the token under test as the cookie. MockMvc keeps
 * no cookie jar between requests, so an old token really is replayed.
 */
class RefreshRotationTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    /** POST /api/auth/refresh with the token as the refresh cookie. */
    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("flowerplus_rt", refreshToken)));
    }

    /** Create an ACTIVE user and log in, returning a live refresh token. */
    private String loginAndGetRefreshToken(String email) throws Exception {
        createUser(email, PASSWORD, UserAccountStatus.ACTIVE);
        return loginTokens(email, PASSWORD).refresh();
    }

    @Test
    @DisplayName("a refresh returns a brand-new refresh token")
    void refreshRotatesTheToken() throws Exception {
        String rt1 = loginAndGetRefreshToken("rotate@example.com");

        String rt2 = Tokens.from(refresh(rt1).andExpect(status().isNoContent()).andReturn()).refresh();

        assertThat(rt2).isNotEqualTo(rt1);
    }

    @Test
    @DisplayName("replaying an already-rotated token is rejected")
    void replayingARotatedTokenIsRejected() throws Exception {
        String rt1 = loginAndGetRefreshToken("replay@example.com");

        refresh(rt1).andExpect(status().isNoContent());   // rotates rt1 away

        refresh(rt1)                                        // rt1 is now a spent tombstone
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("reuse of a spent token wipes the whole family")
    void reuseWipesTheFamily() throws Exception {
        String rt1 = loginAndGetRefreshToken("family@example.com");

        String rt2 = Tokens.from(refresh(rt1).andExpect(status().isNoContent()).andReturn()).refresh();

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

    @Test
    @DisplayName("a valid token sent only in the body counts as missing")
    void tokenInBodyIsIgnored() throws Exception {
        String rt1 = loginAndGetRefreshToken("body@example.com");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("flowerplus_rt", rt1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));

        // Not consumed: the same token still rotates when sent as the cookie.
        refresh(rt1).andExpect(status().isNoContent());
    }
}
