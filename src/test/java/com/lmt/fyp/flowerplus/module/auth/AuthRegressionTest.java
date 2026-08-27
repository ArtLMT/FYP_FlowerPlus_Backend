package com.lmt.fyp.flowerplus.module.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.fake.InMemoryOtpStore;
import com.lmt.fyp.flowerplus.fake.NoOpEmailSender;
import com.lmt.fyp.flowerplus.fake.TestFakesConfig;
import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaRepository;
import com.lmt.fyp.flowerplus.module.auth.web.dto.LoginRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.ResendOtpRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.VerifyOtpRequest;
import com.lmt.fyp.flowerplus.module.email.domain.model.EmailMessage;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaRepository;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserProfileJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "POSTGRES_HOST=localhost",
        "POSTGRES_PORT=5432",
        "POSTGRES_DB=flowerplus",
        "POSTGRES_USER=dev_user",
        "POSTGRES_PASSWORD=dev_password",
        "JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "JWT_EXPIRATION=86400000",
        "EMAIL_HOST=localhost",
        "EMAIL_PORT=1025",
        "EMAIL_USERNAME=test@flowerplus.com",
        "EMAIL_PASSWORD=testpassword"
})
@Import(TestFakesConfig.class)
class AuthRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InMemoryOtpStore inMemoryOtpStore;

    @Autowired
    private NoOpEmailSender noOpEmailSender;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private UserProfileJpaRepository userProfileRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
        inMemoryOtpStore.clear();
        noOpEmailSender.clear();
    }

    @Test
    @DisplayName("End-to-end auth flow regression test")
    void testEndToEndAuthFlow() throws Exception {
        String email = "testuser@example.com";
        String password = "Password123!";
        String fullName = "Test User";

        // ------------------------------------------------------------------ //
        // 1. Register -> 200, account PENDING, OTP issued
        // ------------------------------------------------------------------ //
        RegisterRequest registerRequest = makeRegisterRequest(email, password, fullName);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Please check your email"));

        UserJpaEntity createdUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(createdUser.getStatus()).isEqualTo(UserAccountStatus.PENDING);

        // ------------------------------------------------------------------ //
        // 2. Read the code from the fake store
        // ------------------------------------------------------------------ //
        assertThat(inMemoryOtpStore.findHash(email)).isPresent();
        String otpCode = awaitLatestOtpCode(1);
        assertThat(otpCode).isNotNull().hasSize(6);

        // ------------------------------------------------------------------ //
        // 3. Wrong code x6 -> attempt-cap response & code destroyed
        // ------------------------------------------------------------------ //
        VerifyOtpRequest wrongOtpRequest = makeVerifyOtpRequest(email, "000000");

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongOtpRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("OTP_INVALID"));
        }

        // 6th wrong attempt triggers attempt limit and invalidates OTP
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("OTP_ATTEMPTS_EXCEEDED"));

        // Code destroyed in store
        assertThat(inMemoryOtpStore.findHash(email)).isEmpty();

        // ------------------------------------------------------------------ //
        // 4. Resend inside 60s -> expect 429/204 and no second send
        // ------------------------------------------------------------------ //
        ResendOtpRequest resendRequest = makeResendOtpRequest(email);

        // Calling resend inside cooldown interval (60s)
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("OTP_THROTTLED"));

        // Verify no second email was sent
        assertThat(noOpEmailSender.getSentMessages()).hasSize(1);

        // ------------------------------------------------------------------ //
        // 5. Re-register / Re-issue OTP & Verify -> account ACTIVE
        // ------------------------------------------------------------------ //
        // Clear cooldown map so new OTP can be issued for account verification
        inMemoryOtpStore.clear();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        String validOtpCode = awaitLatestOtpCode(2);
        VerifyOtpRequest validVerifyRequest = makeVerifyOtpRequest(email, validOtpCode);

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowerplus_at").isNotEmpty())
                .andExpect(jsonPath("$.flowerplus_rt").isNotEmpty())
                .andExpect(cookie().exists("flowerplus_at"))
                .andExpect(cookie().exists("flowerplus_rt"));

        UserJpaEntity verifiedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(verifiedUser.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);

        // ------------------------------------------------------------------ //
        // 6. Login -> 200, JWT returned & cookies set
        // ------------------------------------------------------------------ //
        LoginRequest loginRequest = makeLoginRequest(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowerplus_at").isNotEmpty())
                .andExpect(jsonPath("$.flowerplus_rt").isNotEmpty())
                .andExpect(cookie().exists("flowerplus_at"))
                .andExpect(cookie().exists("flowerplus_rt"))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        JsonNode loginNode = objectMapper.readTree(responseJson);
        String accessToken = loginNode.get("flowerplus_at").asText();
        String refreshToken = loginNode.get("flowerplus_rt").asText();

        // ------------------------------------------------------------------ //
        // 7. GET /api/users/{id} with JWT (200) and without (401 ErrorResponse)
        // ------------------------------------------------------------------ //
        UUID userId = verifiedUser.getId();

        // With JWT -> 200
        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value(email));

        // Without JWT -> 401 ErrorResponse shape
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/users/" + userId));

        // ------------------------------------------------------------------ //
        // 8. Refresh via cookie AND via body (both 200)
        // ------------------------------------------------------------------ //
        // Refresh via cookie -> 200
        MvcResult refreshCookieResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("flowerplus_rt", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowerplus_at").isNotEmpty())
                .andExpect(jsonPath("$.flowerplus_rt").isNotEmpty())
                .andExpect(cookie().exists("flowerplus_at"))
                .andExpect(cookie().exists("flowerplus_rt"))
                .andReturn();

        String refreshCookieJson = refreshCookieResult.getResponse().getContentAsString();
        String newRefreshTokenFromCookie = objectMapper.readTree(refreshCookieJson).get("flowerplus_rt").asText();

        // Refresh via body -> 200
        String refreshBodyJson = objectMapper.writeValueAsString(Map.of("flowerplus_rt", newRefreshTokenFromCookie));
        MvcResult refreshBodyResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowerplus_at").isNotEmpty())
                .andExpect(jsonPath("$.flowerplus_rt").isNotEmpty())
                .andExpect(cookie().exists("flowerplus_at"))
                .andExpect(cookie().exists("flowerplus_rt"))
                .andReturn();

        String activeRefreshToken = objectMapper.readTree(refreshBodyResult.getResponse().getContentAsString())
                .get("flowerplus_rt").asText();

        // ------------------------------------------------------------------ //
        // 9. Logout (204, cookies cleared)
        // ------------------------------------------------------------------ //
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("flowerplus_rt", activeRefreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("flowerplus_at", 0))
                .andExpect(cookie().maxAge("flowerplus_rt", 0));

        // ------------------------------------------------------------------ //
        // 10. Reuse the refresh token (401)
        // ------------------------------------------------------------------ //
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("flowerplus_rt", activeRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"));

        // ------------------------------------------------------------------ //
        // 11. Login as status variants: SUSPENDED, BANNED, PENDING
        // ------------------------------------------------------------------ //
        // SUSPENDED -> succeeds (200)
        UserJpaEntity suspendedUser = createUser("suspended@example.com", password, UserAccountStatus.SUSPENDED);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(makeLoginRequest(suspendedUser.getEmail(), password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowerplus_at").isNotEmpty());

        // BANNED -> 403 (ACCOUNT_BLOCKED via LockedException)
        UserJpaEntity bannedUser = createUser("banned@example.com", password, UserAccountStatus.BANNED);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(makeLoginRequest(bannedUser.getEmail(), password))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_BLOCKED"));

        // PENDING -> 403 (ACCOUNT_NOT_VERIFIED via DisabledException)
        UserJpaEntity pendingUser = createUser("pending@example.com", password, UserAccountStatus.PENDING);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(makeLoginRequest(pendingUser.getEmail(), password))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_VERIFIED"));
    }

    private String awaitLatestOtpCode(int expectedSize) throws InterruptedException {
        long endTime = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < endTime) {
            List<EmailMessage> messages = noOpEmailSender.getSentMessages();
            if (messages.size() >= expectedSize) {
                return (String) messages.get(expectedSize - 1).getVariables().get("otp");
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Timed out waiting for OTP email #" + expectedSize);
    }

    private RegisterRequest makeRegisterRequest(String email, String password, String fullName) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFullName(fullName);
        return req;
    }

    private LoginRequest makeLoginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private VerifyOtpRequest makeVerifyOtpRequest(String email, String code) {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(email);
        req.setCode(code);
        return req;
    }

    private ResendOtpRequest makeResendOtpRequest(String email) {
        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(email);
        return req;
    }

    private UserJpaEntity createUser(String email, String rawPassword, UserAccountStatus status) {
        UserJpaEntity user = UserJpaEntity.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.CUSTOMER)
                .status(status)
                .provider(AuthProvider.LOCAL)
                .build();
        UserJpaEntity savedUser = userRepository.save(user);

        userProfileRepository.save(UserProfileJpaEntity.builder()
                .user(savedUser)
                .fullName("User " + status.name())
                .build());

        return savedUser;
    }
}
