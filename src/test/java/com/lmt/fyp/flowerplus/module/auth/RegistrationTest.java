package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for registration edge cases and request handling
 * (items 8, 15, 9) — the branches the happy-path AuthRegressionTest never hits.
 */
class RegistrationTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    @Test
    @DisplayName("re-registering an active email is a 409")
    void duplicateActiveEmailConflicts() throws Exception {
        createUser("active@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("active@example.com", PASSWORD, "Someone"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("a blocked email answers the same 409, leaking nothing")
    void bannedEmailIsIndistinguishable() throws Exception {
        createUser("banned@example.com", PASSWORD, UserAccountStatus.BANNED);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("banned@example.com", PASSWORD, "Someone"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("re-registering a pending account does not overwrite its password")
    void reRegistrationDoesNotOverwritePassword() throws Exception {
        String email = "pending@example.com";
        String firstPassword = "FirstPass123!";
        String secondPassword = "SecondPass456!";

        // First registration -> PENDING with the first password, OTP #1 sent.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest(email, firstPassword, "First"))))
                .andExpect(status().isOk());
        awaitOtp(1);

        // Clear the resend throttle so the second registration can issue a code
        // (otherwise it is rejected as OTP_THROTTLED inside the 60s cooldown).
        inMemoryOtpStore.clear();

        // Second registration with a DIFFERENT password -> new OTP, password NOT changed.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest(email, secondPassword, "Second"))))
                .andExpect(status().isOk());
        String code = awaitOtp(2);

        // Verify with the latest code -> account becomes ACTIVE.
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(verifyRequest(email, code))))
                .andExpect(status().isOk());

        // The FIRST password still works; the second never took effect.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(loginRequest(email, firstPassword))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(loginRequest(email, secondPassword))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("a non-UUID path variable is a 400, not a 500")
    void badPathVariableIsBadRequest() throws Exception {
        createUser("caller@example.com", PASSWORD, UserAccountStatus.ACTIVE);
        String token = loginTokens("caller@example.com", PASSWORD).get("flowerplus_at").asText();

        mockMvc.perform(get("/api/users/not-a-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("a malformed JSON body is a 400, not a 500")
    void malformedBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
