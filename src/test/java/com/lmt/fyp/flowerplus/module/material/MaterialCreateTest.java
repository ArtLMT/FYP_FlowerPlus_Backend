package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers Material creation, input validation, enum values, and duplicate-name handling. */
class MaterialCreateTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-01: Staff creates a trimmed, active material")
    void staffCreatesMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("  Red Rose  ", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Red Rose"))
                .andExpect(jsonPath("$.status").value(MaterialStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.version").value(0));
    }

    @ParameterizedTest(name = "create rejects {1} with {2}")
    @MethodSource("invalidCreateBodies")
    @DisplayName("T-MAT-01/05: Create validates every input boundary")
    void createValidatesInput(String body, String field, String rule) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[?(@.field == '" + field + "' && @.rule == '" + rule + "')]").exists());
    }

    static Stream<Arguments> invalidCreateBodies() {
        String validFields = "\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":15000";
        return Stream.of(
                Arguments.of("{" + validFields + "}", "name", "NotBlank"),
                Arguments.of("{\"name\":\"   \"," + validFields + "}", "name", "NotBlank"),
                Arguments.of("{\"name\":\"" + "a".repeat(101) + "\"," + validFields + "}", "name", "Size"),
                Arguments.of("{\"name\":\"Rose\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":15000}", "type", "NotNull"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"sellingPrice\":15000}", "unitOfMeasure", "NotNull"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\"}", "sellingPrice", "NotNull"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":0}", "sellingPrice", "Min"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":-1}", "sellingPrice", "Min"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":15000.5}", "sellingPrice", "Digits")
        );
    }

    @ParameterizedTest
    @EnumSource(MaterialType.class)
    @DisplayName("T-MAT-01: Every material type is accepted")
    void acceptsEveryMaterialType(MaterialType type) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Material", type, UnitOfMeasure.PIECE, 1000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value(type.name()));
    }

    @ParameterizedTest
    @EnumSource(UnitOfMeasure.class)
    @DisplayName("T-MAT-01: Every unit of measure is accepted")
    void acceptsEveryUnit(UnitOfMeasure unit) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Material", MaterialType.DECORATION, unit, 1000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitOfMeasure").value(unit.name()));
    }

    @Test
    @DisplayName("T-MAT-01: Unknown type and unit values are malformed requests")
    void rejectsUnknownEnumValues() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rose","type":"OTHER","unitOfMeasure":"STEM","sellingPrice":15000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rose","type":"FLOWER","unitOfMeasure":"BOX","sellingPrice":15000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("T-MAT-01: Unparseable JSON is a malformed request")
    void rejectsUnparseableJson() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rose\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("T-MAT-01/05: Duplicate names are compared after trimming and ignoring case")
    void rejectsNormalizedDuplicateName() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("  red rose  ", MaterialType.DECORATION, UnitOfMeasure.PIECE, 20000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NAME_EXISTS"));
    }
}
