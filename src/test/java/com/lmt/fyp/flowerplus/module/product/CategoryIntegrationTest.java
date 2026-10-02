package com.lmt.fyp.flowerplus.module.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.fake.TestFakesConfig;
import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        "EMAIL_PASSWORD=testpassword",
        "ADMIN_EMAIL=admin@flowerplus.test",
        "ADMIN_PASSWORD=AdminPassword123"
})
@Import(TestFakesConfig.class)
class CategoryIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String STAFF_EMAIL = "product-staff@example.com";
    private static final String ADMIN_EMAIL = "product-admin@example.com";
    private static final String CUSTOMER_EMAIL = "product-customer@example.com";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        jdbcTemplate.update("DELETE FROM product_category");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM category");
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("T-PROD-01: Public category list is stable and omits management version")
    void publicListOmitsVersionAndUsesNormalizedNameOrder() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createCategory(staff, " wedding ");
        createCategory(staff, "Birthday");

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Birthday"))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].version").doesNotExist())
                .andExpect(jsonPath("$[1].name").value("wedding"));
    }

    @Test
    @DisplayName("T-PROD-01: Staff creates trimmed category at version zero")
    void createCategoryReturnsManagementVersionAndLocation() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/categories")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "  Birthday  "))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Birthday"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("T-PROD-01: Category names are required, limited, and unique after trim/case folding")
    void validatesCategoryNameAndEnforcesCaseInsensitiveUniqueness() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createCategory(staff, "Birthday");

        mockMvc.perform(post("/api/manage/categories")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", " birthday "))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NAME_EXISTS"));

        mockMvc.perform(post("/api/manage/categories")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields[0].field").value("name"));

        mockMvc.perform(post("/api/manage/categories")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "x".repeat(101)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields[0].field").value("name"));
    }

    @Test
    @DisplayName("T-PROD-01: Management reads include version; rename increments it and rejects stale input")
    void managementReadsAndRenamesUseOptimisticVersion() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createCategory(staff, "Birthday");

        mockMvc.perform(get("/api/manage/categories/{id}", id)
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(put("/api/manage/categories/{id}", id)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Wedding", "version", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wedding"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/manage/categories/{id}", id)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Anniversary", "version", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("T-PROD-01: Deleting a category that would orphan Active products is atomic and lists IDs")
    void deleteRejectsAndReturnsActiveOrphanIds() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID categoryId = createCategory(staff, "Wedding");
        UUID productId = createProduct("ACTIVE", "Bouquet", categoryId);

        mockMvc.perform(delete("/api/manage/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_CATEGORY_IN_USE"))
                .andExpect(jsonPath("$.details.productIds[0]").value(productId.toString()));

        org.assertj.core.api.Assertions.assertThat(categoryExists(categoryId)).isTrue();
        org.assertj.core.api.Assertions.assertThat(categoryLinkExists(productId, categoryId)).isTrue();
        org.assertj.core.api.Assertions.assertThat(productVersion(productId)).isZero();
    }

    @Test
    @DisplayName("T-PROD-01: Deletion unlinks hidden products and advances their Product versions")
    void deleteUnlinksDraftAndDeactivatedProducts() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID categoryId = createCategory(staff, "Occasion");
        UUID draftId = createProduct("DRAFT", "Draft bouquet", categoryId);
        UUID deactivatedId = createProduct("DEACTIVATED", "Old bouquet", categoryId);

        mockMvc.perform(delete("/api/manage/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 0))))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(categoryExists(categoryId)).isFalse();
        org.assertj.core.api.Assertions.assertThat(categoryLinkExists(draftId, categoryId)).isFalse();
        org.assertj.core.api.Assertions.assertThat(categoryLinkExists(deactivatedId, categoryId)).isFalse();
        org.assertj.core.api.Assertions.assertThat(productVersion(draftId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(productVersion(deactivatedId)).isEqualTo(1);
    }

    @Test
    @DisplayName("T-PROD-01: Active product with another category remains valid after deletion")
    void deleteCanUnlinkActiveProductWhenAnotherCategoryRemains() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID deletedCategory = createCategory(staff, "Wedding");
        UUID remainingCategory = createCategory(staff, "Birthday");
        UUID productId = createProduct("ACTIVE", "Bouquet", deletedCategory, remainingCategory);

        mockMvc.perform(delete("/api/manage/categories/{id}", deletedCategory)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 0))))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(categoryLinkExists(productId, deletedCategory)).isFalse();
        org.assertj.core.api.Assertions.assertThat(categoryLinkExists(productId, remainingCategory)).isTrue();
        org.assertj.core.api.Assertions.assertThat(productVersion(productId)).isEqualTo(1);
    }

    @Test
    @DisplayName("T-PROD-01: Management is Staff/Admin only; public category listing allows Guest")
    void enforcesCategoryPermissions() throws Exception {
        String admin = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customer = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/manage/categories")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/manage/categories")
                        .header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manage/categories"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-PROD-01: Stale and missing category deletes return the documented conflicts/not-found")
    void deleteChecksCategoryVersionAndMissingId() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createCategory(staff, "Ceremony");
        mockMvc.perform(put("/api/manage/categories/{id}", id)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Celebration", "version", 0))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/manage/categories/{id}", id)
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));

        mockMvc.perform(get("/api/manage/categories/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isNotFound());
    }

    private UUID createCategory(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/manage/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText());
    }

    private UUID createProduct(String productStatus, String name, UUID... categoryIds) {
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO product (id, name, price, product_type, status, created_at, updated_at, version)
                        VALUES (?, ?, ?, 'PRE_ORDER', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                        """,
                productId, name, new BigDecimal("15000"), productStatus);
        for (UUID categoryId : categoryIds) {
            jdbcTemplate.update("INSERT INTO product_category(product_id, category_id) VALUES (?, ?)",
                    productId, categoryId);
        }
        return productId;
    }

    private String tokenFor(String email, UserRole role) throws Exception {
        userProfileRepository.save(UserProfile.builder()
                .user(userRepository.save(User.builder()
                        .username(email)
                        .email(email)
                        .password(passwordEncoder.encode(PASSWORD))
                        .role(role)
                        .status(UserAccountStatus.ACTIVE)
                        .provider(AuthProvider.LOCAL)
                        .build()))
                .fullName("Product Test User")
                .build());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();
        return result.getResponse().getCookie("flowerplus_at").getValue();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private boolean categoryExists(UUID categoryId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) > 0 FROM category WHERE id = ?", Boolean.class, categoryId));
    }

    private boolean categoryLinkExists(UUID productId, UUID categoryId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) > 0 FROM product_category WHERE product_id = ? AND category_id = ?",
                Boolean.class, productId, categoryId));
    }

    private long productVersion(UUID productId) {
        return jdbcTemplate.queryForObject("SELECT version FROM product WHERE id = ?", Long.class, productId);
    }
}
