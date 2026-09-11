package com.lmt.fyp.flowerplus.module.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.fake.TestFakesConfig;
import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.AddressRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import com.lmt.fyp.flowerplus.module.user.web.dto.AddressRequest;
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

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Property block matches AuthIntegrationSupport so both suites share one cached context. */
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
        "EMAIL_PASSWORD=testpassword",
        "ADMIN_EMAIL=admin@flowerplus.test",
        "ADMIN_PASSWORD=AdminPassword123"
})
@Import(TestFakesConfig.class)
abstract class AddressIntegrationSupport {

    protected static final String PASSWORD = "Password123!";

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected AddressRepository addressRepository;
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
        // Addresses first: they reference user_account.
        addressRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected User createUser(String email) {
        User saved = userRepository.save(User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(UserRole.CUSTOMER)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(saved)
                .fullName("Test User")
                .build());
        return saved;
    }

    /** Create an ACTIVE account and return a bearer token for it. */
    protected String tokenFor(String email) throws Exception {
        createUser(email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("flowerplus_at").asText();
    }

    protected AddressRequest request(String receiverName, boolean isDefault) {
        return new AddressRequest(receiverName, "0912345678", receiverName + " street", isDefault);
    }

    /** POST an address and return the id from the created resource. */
    protected UUID createAddress(String token, String receiverName, boolean isDefault) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(receiverName, isDefault))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }
}
