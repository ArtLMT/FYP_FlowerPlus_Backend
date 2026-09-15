package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for POST /api/auth/forgot-password and /reset-password. */
class PasswordResetTest extends AuthIntegrationSupport {

    private static final String EMAIL = "reset@example.com";
    private static final String OLD_PASSWORD = "OldPassword123";
    private static final String NEW_PASSWORD = "NewPassword456";

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
    }

    private ResultActions reset(String email, String code, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "code", code, "newPassword", newPassword))));
    }

    @Test
    @DisplayName("an active account resets its password with the emailed code")
    void activeAccountResets() throws Exception {
        createUser(EMAIL, OLD_PASSWORD, UserAccountStatus.ACTIVE);

        requestReset(EMAIL).andExpect(status().isNoContent());
        reset(EMAIL, awaitOtp(1), NEW_PASSWORD).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(loginRequest(EMAIL, OLD_PASSWORD))))
                .andExpect(status().isUnauthorized());
        loginTokens(EMAIL, NEW_PASSWORD);
    }

    @Test
    @DisplayName("the reset email uses the reset wording")
    void resetEmailWording() throws Exception {
        createUser(EMAIL, OLD_PASSWORD, UserAccountStatus.ACTIVE);

        requestReset(EMAIL).andExpect(status().isNoContent());
        awaitOtp(1);

        assertThat(noOpEmailSender.getSentMessages().get(0).getSubject()).contains("password reset");
    }

    @Test
    @DisplayName("a reset ends every existing session")
    void resetEndsSessions() throws Exception {
        createUser(EMAIL, OLD_PASSWORD, UserAccountStatus.ACTIVE);
        String refreshToken = loginTokens(EMAIL, OLD_PASSWORD).get("flowerplus_rt").asText();

        requestReset(EMAIL).andExpect(status().isNoContent());
        reset(EMAIL, awaitOtp(1), NEW_PASSWORD).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("flowerplus_rt", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("unknown, unverified and banned emails get the same 204 and no code")
    void ineligibleEmailsGetNoCode() throws Exception {
        createUser("pending@example.com", OLD_PASSWORD, UserAccountStatus.PENDING);
        createUser("banned@example.com", OLD_PASSWORD, UserAccountStatus.BANNED);

        for (String email : List.of("nobody@example.com", "pending@example.com", "banned@example.com")) {
            requestReset(email).andExpect(status().isNoContent());
            assertThat(inMemoryOtpStore.findHash(OtpPurpose.PASSWORD_RESET, email)).isEmpty();
        }
    }

    @Test
    @DisplayName("a quick second request is limited even for an email with no account")
    void limitDoesNotRevealAccounts() throws Exception {
        requestReset("nobody@example.com").andExpect(status().isNoContent());

        requestReset("nobody@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("OTP_THROTTLED"))
                .andExpect(jsonPath("$.details.retryAfterSeconds").isNumber());
    }

    @Test
    @DisplayName("past the daily limit the answer has its own code, even for an email with no account")
    void dailyLimitHasItsOwnCode() throws Exception {
        for (int i = 0; i < 5; i++) {
            requestReset("nobody@example.com").andExpect(status().isNoContent());
            inMemoryOtpStore.clearResendCooldowns();
        }

        requestReset("nobody@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("OTP_DAILY_LIMIT_REACHED"))
                .andExpect(jsonPath("$.details.retryAfterSeconds").isNumber());
    }

    @Test
    @DisplayName("a wrong code is rejected and the password stays the same")
    void wrongCodeIsRejected() throws Exception {
        createUser(EMAIL, OLD_PASSWORD, UserAccountStatus.ACTIVE);
        requestReset(EMAIL).andExpect(status().isNoContent());
        awaitOtp(1);

        reset(EMAIL, "000000", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("OTP_INVALID"));
        loginTokens(EMAIL, OLD_PASSWORD);
    }

    @Test
    @DisplayName("a new password outside the password policy is rejected")
    void weakNewPasswordIsRejected() throws Exception {
        reset(EMAIL, "123456", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[?(@.field == 'newPassword')].rule").value(hasItem("Size")));
    }
}
