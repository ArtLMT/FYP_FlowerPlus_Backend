package com.lmt.fyp.flowerplus.module.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.fake.InMemoryOtpStore;
import com.lmt.fyp.flowerplus.fake.NoOpEmailSender;
import com.lmt.fyp.flowerplus.fake.TestFakesConfig;
import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
import com.lmt.fyp.flowerplus.module.auth.web.dto.LoginRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.VerifyOtpRequest;
import com.lmt.fyp.flowerplus.module.email.service.EmailMessage;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for the auth integration tests. Boots the full app
 * ({@code @SpringBootTest}) with the in-memory OTP store and no-op email sender
 * from {@link TestFakesConfig}, and gives subclasses a clean database before
 * each test plus a few flow helpers. Abstract and not named *Test, so the test
 * runner does not execute it on its own. Every subclass reuses one cached
 * Spring context because the configuration here is identical.
 */
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
abstract class AuthIntegrationSupport {

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected InMemoryOtpStore inMemoryOtpStore;
    @Autowired
    protected NoOpEmailSender noOpEmailSender;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected UserProfileRepository userProfileRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void baseSetUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        // A clean slate before every test so they never leak into each other.
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
        inMemoryOtpStore.clear();
        noOpEmailSender.clear();
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    /** Persist a user directly in a chosen status — the fast path when a test
     *  needs an existing account without walking the register/verify flow. */
    protected User createUser(String email, String rawPassword, UserAccountStatus status) {
        User user = User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.CUSTOMER)
                .status(status)
                .provider(AuthProvider.LOCAL)
                .build();
        User saved = userRepository.save(user);
        userProfileRepository.save(UserProfile.builder()
                .user(saved)
                .fullName("Test " + status.name())
                .build());
        return saved;
    }

    /** Log in and return the parsed response body (fields flowerplus_at, flowerplus_rt).
     *  Expects success — use it only for accounts you know can authenticate. */
    protected JsonNode loginTokens(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** The OTP email delivery is async (AFTER_COMMIT), so poll briefly for the
     *  Nth email and return the plaintext code it carried. */
    protected String awaitOtp(int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            List<EmailMessage> messages = noOpEmailSender.getSentMessages();
            if (messages.size() >= expectedCount) {
                return (String) messages.get(expectedCount - 1).getVariables().get("otp");
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Timed out waiting for OTP email #" + expectedCount);
    }

    protected RegisterRequest registerRequest(String email, String password, String fullName) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFullName(fullName);
        return req;
    }

    protected VerifyOtpRequest verifyRequest(String email, String code) {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(email);
        req.setCode(code);
        return req;
    }

    protected LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }
}
