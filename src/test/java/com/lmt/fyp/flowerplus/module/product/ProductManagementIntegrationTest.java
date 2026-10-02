package com.lmt.fyp.flowerplus.module.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.fake.TestFakesConfig;
import com.lmt.fyp.flowerplus.module.auth.repository.RefreshTokenRepository;
import com.lmt.fyp.flowerplus.module.material.entity.Material;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import com.lmt.fyp.flowerplus.module.material.repository.MaterialRepository;
import com.lmt.fyp.flowerplus.module.product.dto.CreateProductRecipeLineRequest;
import com.lmt.fyp.flowerplus.module.product.dto.CreateProductRequest;
import com.lmt.fyp.flowerplus.module.product.entity.Category;
import com.lmt.fyp.flowerplus.module.product.entity.ProductStatus;
import com.lmt.fyp.flowerplus.module.product.entity.ProductType;
import com.lmt.fyp.flowerplus.module.product.repository.CategoryRepository;
import com.lmt.fyp.flowerplus.module.product.repository.ProductRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

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
class ProductManagementIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String STAFF_EMAIL = "product-management-staff@example.com";
    private static final String ADMIN_EMAIL = "product-management-admin@example.com";
    private static final String CUSTOMER_EMAIL = "product-management-customer@example.com";

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
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        jdbcTemplate.update("DELETE FROM product_recipe");
        jdbcTemplate.update("DELETE FROM product_category");
        jdbcTemplate.update("DELETE FROM product");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.update("DELETE FROM material");
        refreshTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("T-PROD-02: Staff creates a minimal Draft with version zero")
    void staffCanCreateMinimalDraftAndReadIt() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        MvcResult created = mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(draft("  Spring bouquet  ", ProductType.PRE_ORDER))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Spring bouquet"))
                .andExpect(jsonPath("$.type").value("PRE_ORDER"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.basePrice").value(nullValue()))
                .andExpect(jsonPath("$.recipe").isEmpty())
                .andExpect(jsonPath("$.categories").isEmpty())
                .andExpect(jsonPath("$.images").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andExpect(jsonPath("$.updatedBy").isNotEmpty())
                .andReturn();

        UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());
        mockMvc.perform(get("/api/manage/products/{id}", id)
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("T-PROD-02: Admin can create products and customers or guests cannot")
    void productManagementRequiresStaffOrAdmin() throws Exception {
        String admin = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customer = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);
        CreateProductRequest request = draft("Peony", ProductType.PRE_MADE);

        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/manage/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manage/products"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manage/products")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T-PROD-02: Supplied categories and recipe are returned with material facts")
    void draftCanIncludeValidCategoriesAndRecipe() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        Category category = categoryRepository.saveAndFlush(new Category("Wedding"));
        Material stem = saveMaterial("White rose", UnitOfMeasure.STEM, MaterialStatus.ACTIVE);
        Material ribbon = saveMaterial("Satin ribbon", UnitOfMeasure.METRE, MaterialStatus.ACTIVE);

        MvcResult result = mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Bridal bouquet",
                                ProductType.PRE_ORDER,
                                "White roses with ribbon",
                                new BigDecimal("250000"),
                                List.of(category.getId()),
                                List.of(
                                        new CreateProductRecipeLineRequest(stem.getId(), new BigDecimal("12")),
                                        new CreateProductRecipeLineRequest(ribbon.getId(), new BigDecimal("1.25"))
                                )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.basePrice").value(250000))
                .andExpect(jsonPath("$.categories[0].id").value(category.getId().toString()))
                .andExpect(jsonPath("$.categories[0].name").value("Wedding"))
                .andExpect(jsonPath("$.recipe.length()").value(2))
                .andExpect(jsonPath("$.images").isEmpty())
                .andReturn();

        JsonNode recipe = objectMapper.readTree(result.getResponse().getContentAsString()).get("recipe");
        JsonNode stemLine = findRecipeLine(recipe, stem.getId());
        JsonNode ribbonLine = findRecipeLine(recipe, ribbon.getId());
        org.junit.jupiter.api.Assertions.assertEquals("STEM", stemLine.get("materialUnit").asText());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", stemLine.get("materialStatus").asText());
        org.junit.jupiter.api.Assertions.assertEquals("1.25", ribbonLine.get("quantityRequired").asText());
        org.junit.jupiter.api.Assertions.assertEquals("METRE", ribbonLine.get("materialUnit").asText());
    }

    @Test
    @DisplayName("T-PROD-02: Draft fields are optional but supplied values are validated")
    void rejectsBlankDescriptionFractionalPriceAndInvalidRecipePrecision() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        assertValidation(staff, new CreateProductRequest("Rose", ProductType.PRE_ORDER, "   ", null, List.of(), List.of()), "description");
        assertValidation(staff, new CreateProductRequest("Rose", ProductType.PRE_ORDER, null, new BigDecimal("1200.5"), List.of(), List.of()), "basePrice");
        assertValidation(staff, new CreateProductRequest("Rose", ProductType.PRE_ORDER, null, BigDecimal.ZERO, List.of(), List.of()), "basePrice");

        Material stem = saveMaterial("Red tulip", UnitOfMeasure.STEM, MaterialStatus.ACTIVE);
        assertValidation(staff, new CreateProductRequest(
                "Rose", ProductType.PRE_ORDER, null, null, List.of(),
                List.of(new CreateProductRecipeLineRequest(stem.getId(), new BigDecimal("1.001")))),
                "quantityRequired");
    }

    @Test
    @DisplayName("T-PROD-02: Only Active materials can be added and non-METRE quantities are whole")
    void rejectsInactiveMaterialsAndFractionalNonMetreQuantities() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        Material inactive = saveMaterial("Dry rose", UnitOfMeasure.STEM, MaterialStatus.DEACTIVATED);

        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Rose", ProductType.PRE_MADE, null, null, List.of(),
                                List.of(new CreateProductRecipeLineRequest(inactive.getId(), BigDecimal.ONE))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_RECIPE_MATERIAL_INACTIVE"));

        Material activeStem = saveMaterial("Fresh rose", UnitOfMeasure.STEM, MaterialStatus.ACTIVE);
        MvcResult invalidFraction = mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Rose", ProductType.PRE_MADE, null, null, List.of(),
                                List.of(new CreateProductRecipeLineRequest(activeStem.getId(), new BigDecimal("1.5")))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[0].field").value("recipe[0].quantityRequired"))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertFalse(invalidFraction.getResponse().getContentAsString().isBlank());
        org.junit.jupiter.api.Assertions.assertEquals(0, productRepository.count());
    }

    @Test
    @DisplayName("T-PROD-02: Recipe references lock material type and unit, but allow other corrections")
    void recipeReferencePreventsMaterialUnitOrTypeChange() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        Material material = saveMaterial("Cream rose", UnitOfMeasure.STEM, MaterialStatus.ACTIVE);
        UUID productId = createProductWithRecipe(staff, material.getId(), new BigDecimal("6"));

        mockMvc.perform(put("/api/manage/materials/{id}", material.getId())
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cream rose","type":"DECORATION","unitOfMeasure":"PIECE","sellingPrice":1200,"version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_IN_USE"));

        mockMvc.perform(put("/api/manage/materials/{id}", material.getId())
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Cream garden rose","type":"FLOWER","unitOfMeasure":"STEM","sellingPrice":1200,"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cream garden rose"))
                .andExpect(jsonPath("$.unitOfMeasure").value("STEM"));

        mockMvc.perform(get("/api/manage/products/{id}", productId)
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipe[0].materialName").value("Cream garden rose"));
    }

    @Test
    @DisplayName("T-PROD-02: Draft category links coordinate with the category deletion guard")
    void deletingCategoryUnlinksDraftAndAdvancesItsVersion() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        Category category = categoryRepository.saveAndFlush(new Category("Graduation"));
        UUID productId = mockProductWithCategory(staff, category.getId());

        mockMvc.perform(delete("/api/manage/categories/{id}", category.getId())
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/manage/products/{id}", productId)
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isEmpty())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("T-PROD-02: Invalid references and duplicate recipe lines do not save a partial Product")
    void invalidReferencesAndDuplicateMaterialsAreRejectedAtomically() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        Material material = saveMaterial("Carnation", UnitOfMeasure.STEM, MaterialStatus.ACTIVE);
        long initialProducts = productRepository.count();

        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Arrangement", ProductType.PRE_ORDER, null, null,
                                List.of(UUID.randomUUID()), List.of()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Arrangement", ProductType.PRE_ORDER, null, null, List.of(),
                                List.of(new CreateProductRecipeLineRequest(UUID.randomUUID(), BigDecimal.ONE))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NOT_FOUND"));
        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Arrangement", ProductType.PRE_ORDER, null, null, List.of(),
                                List.of(
                                        new CreateProductRecipeLineRequest(material.getId(), BigDecimal.ONE),
                                        new CreateProductRecipeLineRequest(material.getId(), BigDecimal.TEN))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        org.junit.jupiter.api.Assertions.assertEquals(initialProducts, productRepository.count());
    }

    @Test
    @DisplayName("T-PROD-02: Management list is paged, searchable, stable, and does not hide statuses")
    void managementListIncludesAllStatesAndUsesPageResponse() throws Exception {
        String staff = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createDraft(staff, "Autumn arrangement", ProductType.PRE_ORDER);
        UUID deactivated = createDraft(staff, "Winter wreath", ProductType.PRE_MADE);
        jdbcTemplate.update("UPDATE product SET status = 'DEACTIVATED' WHERE id = ?", deactivated);

        mockMvc.perform(get("/api/manage/products")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/manage/products")
                        .param("search", "WINTER")
                        .header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("DEACTIVATED"));
    }

    private CreateProductRequest draft(String name, ProductType type) {
        return new CreateProductRequest(name, type, null, null, List.of(), List.of());
    }

    private void assertValidation(String token, CreateProductRequest request, String fieldFragment) throws Exception {
        mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[0].field").value(org.hamcrest.Matchers.containsString(fieldFragment)));
    }

    private UUID createDraft(String token, String name, ProductType type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(draft(name, type))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createProductWithRecipe(String token, UUID materialId, BigDecimal quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Recipe Product", ProductType.PRE_ORDER, null, null, List.of(),
                                List.of(new CreateProductRecipeLineRequest(materialId, quantity))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID mockProductWithCategory(String token, UUID categoryId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/manage/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateProductRequest(
                                "Category Product", ProductType.PRE_MADE, null, null, List.of(categoryId), List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private Material saveMaterial(String name, UnitOfMeasure unit, MaterialStatus status) {
        Material material = new Material(name, MaterialType.FLOWER, unit, new BigDecimal("1000"));
        material.setStatus(status);
        return materialRepository.saveAndFlush(material);
    }

    private JsonNode findRecipeLine(JsonNode lines, UUID materialId) {
        for (JsonNode line : lines) {
            if (line.get("materialId").asText().equals(materialId.toString())) {
                return line;
            }
        }
        throw new AssertionError("Recipe material not found: " + materialId);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String tokenFor(String email, UserRole role) throws Exception {
        User saved = userRepository.save(User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build());
        userProfileRepository.save(UserProfile.builder().user(saved).fullName("Test User").build());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();
        return result.getResponse().getCookie("flowerplus_at").getValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
