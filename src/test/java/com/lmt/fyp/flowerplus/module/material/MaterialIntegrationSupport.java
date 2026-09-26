package com.lmt.fyp.flowerplus.module.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.fake.TestFakesConfig;
import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
import com.lmt.fyp.flowerplus.module.material.dto.CreateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.dto.UpdateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import com.lmt.fyp.flowerplus.module.material.repository.MaterialRepository;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "EMAIL_PASSWORD=testpassword",
        "ADMIN_EMAIL=admin@flowerplus.test",
        "ADMIN_PASSWORD=AdminPassword123"
})
@Import(TestFakesConfig.class)
/* Provides shared application setup, authentication, cleanup, and request helpers for Material tests. */
public abstract class MaterialIntegrationSupport {

    protected static final String PASSWORD = "Password123!";
    protected static final String STAFF_EMAIL = "staff@example.com";
    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String CUSTOMER_EMAIL = "customer@example.com";

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected MaterialRepository materialRepository;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
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
        materialRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected User createUser(String email, UserRole role) {
        User saved = userRepository.save(User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(saved)
                .fullName("Test User")
                .build());
        return saved;
    }

    protected String tokenFor(String email, UserRole role) throws Exception {
        createUser(email, role);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();
        return result.getResponse().getCookie("flowerplus_at").getValue();
    }

    protected CreateMaterialRequest request(String name, MaterialType type, UnitOfMeasure uom, int price) {
        return new CreateMaterialRequest(name, type, uom, BigDecimal.valueOf(price));
    }

    protected UUID createMaterialViaApi(String token, String name, MaterialType type, UnitOfMeasure uom, int price) throws Exception {
        return UUID.fromString(createMaterialResponseViaApi(token, name, type, uom, price).get("id").asText());
    }

    protected JsonNode createMaterialResponseViaApi(
            String token, String name, MaterialType type, UnitOfMeasure uom, int price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(name, type, uom, price))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected UpdateMaterialRequest updateRequest(
            String name, MaterialType type, UnitOfMeasure uom, int price, long version) {
        return new UpdateMaterialRequest(name, type, uom, BigDecimal.valueOf(price), version);
    }
}
