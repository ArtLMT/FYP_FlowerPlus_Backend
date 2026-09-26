package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers Material viewing, filtering, searching, sorting, and pagination. */
class MaterialQueryTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-04: Staff views one material")
    void viewsMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(get("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    @DisplayName("T-MAT-04: Viewing a missing material returns MATERIAL_NOT_FOUND")
    void viewMissingMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(get("/api/manage/materials/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NOT_FOUND"));
    }

    @Test
    @DisplayName("T-MAT-04: Unfiltered list includes active and deactivated materials")
    void listIncludesEveryStatus() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Active Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        UUID deactivated = createMaterialViaApi(
                token, "Old Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 14000);
        mockMvc.perform(put("/api/manage/materials/" + deactivated + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manage/materials")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].status",
                        containsInAnyOrder("ACTIVE", "DEACTIVATED")));
    }

    @Test
    @DisplayName("T-MAT-04/07: Type and status filters compose with name search")
    void filtersComposeWithSearch() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        UUID whiteRose = createMaterialViaApi(
                token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000);
        createMaterialViaApi(token, "White Ribbon", MaterialType.DECORATION, UnitOfMeasure.METRE, 5000);
        mockMvc.perform(put("/api/manage/materials/" + whiteRose + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manage/materials").param("type", "DECORATION")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("White Ribbon"));

        mockMvc.perform(get("/api/manage/materials").param("status", "DEACTIVATED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("White Rose"));

        mockMvc.perform(get("/api/manage/materials")
                        .param("search", "WHITE")
                        .param("type", "FLOWER")
                        .param("status", "DEACTIVATED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("White Rose"));
    }

    @Test
    @DisplayName("T-MAT-07: SQL wildcard characters are literal search text")
    void wildcardCharactersAreLiteral() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "50% Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        createMaterialViaApi(token, "Rose_Stem", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 17000);

        mockMvc.perform(get("/api/manage/materials").param("search", "%")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("50% Rose"));

        mockMvc.perform(get("/api/manage/materials").param("search", "_")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Rose_Stem"));
    }

    @ParameterizedTest(name = "{0} starts with {1}")
    @CsvSource({
            "NAME_ASC,Alpha",
            "NAME_DESC,Charlie",
            "PRICE_ASC,Bravo",
            "PRICE_DESC,Alpha",
            "CREATED_NEWEST,Charlie",
            "CREATED_OLDEST,Alpha"
    })
    @DisplayName("T-MAT-07: Every approved sort option selects its fixed order")
    void usesEveryApprovedSort(String sort, String expectedFirst) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID alpha = createMaterialViaApi(token, "Alpha", MaterialType.FLOWER, UnitOfMeasure.STEM, 300);
        UUID bravo = createMaterialViaApi(token, "Bravo", MaterialType.FLOWER, UnitOfMeasure.STEM, 100);
        UUID charlie = createMaterialViaApi(token, "Charlie", MaterialType.FLOWER, UnitOfMeasure.STEM, 200);
        jdbcTemplate.update("UPDATE material SET created_at = TIMESTAMPTZ '2026-01-01 00:00:00Z' WHERE id = ?", alpha);
        jdbcTemplate.update("UPDATE material SET created_at = TIMESTAMPTZ '2026-01-02 00:00:00Z' WHERE id = ?", bravo);
        jdbcTemplate.update("UPDATE material SET created_at = TIMESTAMPTZ '2026-01-03 00:00:00Z' WHERE id = ?", charlie);

        mockMvc.perform(get("/api/manage/materials").param("sort", sort)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(expectedFirst));
    }

    @Test
    @DisplayName("T-MAT-07: Equal primary sort values use id ascending")
    void usesIdAsStableTieBreaker() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        insertMaterial(firstId, "Zulu", 1000);
        insertMaterial(secondId, "Alpha", 1000);

        mockMvc.perform(get("/api/manage/materials").param("sort", "PRICE_ASC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$.content[1].id").value(secondId.toString()));
    }

    @Test
    @DisplayName("T-MAT-04/07: Default paging uses 20 items and name ascending")
    void usesDefaultPageAndOrder() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        for (int index = 20; index >= 0; index--) {
            createMaterialViaApi(token, "Material %02d".formatted(index),
                    MaterialType.DECORATION, UnitOfMeasure.PIECE, 1000 + index);
        }

        mockMvc.perform(get("/api/manage/materials")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.content[0].name").value("Material 00"))
                .andExpect(jsonPath("$.content[19].name").value("Material 19"));
    }

    @Test
    @DisplayName("T-MAT-07: Missing and unsupported sorts use NAME_ASC")
    void missingAndUnsupportedSortUseDefault() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(get("/api/manage/materials")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Red Rose"));

        mockMvc.perform(get("/api/manage/materials").param("sort", "sellingPrice,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Red Rose"));
    }

    @Test
    @DisplayName("T-MAT-07: Page-size boundaries 1 and 100 are accepted")
    void acceptsPageSizeBoundaries() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(get("/api/manage/materials").param("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/api/manage/materials").param("size", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,101"})
    @DisplayName("T-MAT-07: Invalid page bounds return VALIDATION_FAILED")
    void rejectsInvalidPageBounds(String parameter, String value) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(get("/api/manage/materials").param(parameter, value)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @ParameterizedTest
    @CsvSource({"page,not-a-number", "type,OTHER", "status,REMOVED"})
    @DisplayName("T-MAT-04/07: Wrongly typed query parameters are malformed")
    void rejectsMalformedQueryParameters(String parameter, String value) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(get("/api/manage/materials").param(parameter, value)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    private void insertMaterial(UUID id, String name, int price) {
        jdbcTemplate.update("""
                INSERT INTO material
                    (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at, version)
                VALUES (?, ?, 'DECORATION', 'PIECE', ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """, id, name, price);
    }
}
