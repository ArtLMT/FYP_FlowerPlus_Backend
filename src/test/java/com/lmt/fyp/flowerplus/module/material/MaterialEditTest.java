package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers Material editing, normalization, validation, and missing-resource behavior. */
class MaterialEditTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-02: Staff edits a material and the saved name is trimmed")
    void editsMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "  White Rose  ", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("White Rose"))
                .andExpect(jsonPath("$.sellingPrice").value(16000))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("T-MAT-02: A deactivated material remains editable")
    void editsDeactivatedMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "White Rose", MaterialType.DECORATION, UnitOfMeasure.PIECE, 16000, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("White Rose"))
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }

    @Test
    @DisplayName("T-MAT-02: Keeping or re-casing a material's own name is not a duplicate")
    void ownNameIsNotDuplicate() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Red Rose"));

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "red rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("red rose"));
    }

    @Test
    @DisplayName("T-MAT-02: Renaming to another material's normalized name is refused")
    void editRejectsAnotherMaterialsName() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        UUID whiteRose = createMaterialViaApi(
                token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000);

        mockMvc.perform(put("/api/manage/materials/" + whiteRose)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "  RED ROSE ", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000, 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NAME_EXISTS"));
    }

    @Test
    @DisplayName("T-MAT-02: Editing a missing material returns MATERIAL_NOT_FOUND")
    void editMissingMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(put("/api/manage/materials/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000, 0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NOT_FOUND"));
    }

    @ParameterizedTest(name = "edit rejects {1} with {2}")
    @MethodSource("invalidEditBodies")
    @DisplayName("T-MAT-02/05: Edit validates every input boundary")
    void editValidatesInput(String body, String field, String rule) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[?(@.field == '" + field + "' && @.rule == '" + rule + "')]").exists());
    }

    static Stream<Arguments> invalidEditBodies() {
        String validFields = "\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":15000,\"version\":0";
        return Stream.of(
                Arguments.of("{" + validFields + "}", "name", "NotBlank"),
                Arguments.of("{\"name\":\"   \"," + validFields + "}", "name", "NotBlank"),
                Arguments.of("{\"name\":\"" + "a".repeat(101) + "\"," + validFields + "}", "name", "Size"),
                Arguments.of("{\"name\":\"Rose\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":15000,\"version\":0}", "type", "NotNull"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"sellingPrice\":15000,\"version\":0}", "unitOfMeasure", "NotNull"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"version\":0}", "sellingPrice", "NotNull"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":0,\"version\":0}", "sellingPrice", "Min"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":-1,\"version\":0}", "sellingPrice", "Min"),
                Arguments.of("{\"name\":\"Rose\",\"type\":\"FLOWER\",\"unitOfMeasure\":\"STEM\",\"sellingPrice\":15000.5,\"version\":0}", "sellingPrice", "Digits")
        );
    }

    @ParameterizedTest
    @EnumSource(MaterialType.class)
    @DisplayName("T-MAT-02: Edit accepts every material type")
    void editAcceptsEveryMaterialType(MaterialType type) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Material", MaterialType.FLOWER, UnitOfMeasure.STEM, 1000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest("Updated Material", type, UnitOfMeasure.STEM, 1000, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(type.name()));
    }

    @ParameterizedTest
    @EnumSource(UnitOfMeasure.class)
    @DisplayName("T-MAT-02: Edit accepts every unit of measure")
    void editAcceptsEveryUnit(UnitOfMeasure unit) throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Material", MaterialType.DECORATION, UnitOfMeasure.PIECE, 1000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest("Updated Material", MaterialType.DECORATION, unit, 1000, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitOfMeasure").value(unit.name()));
    }

    @Test
    @DisplayName("T-MAT-02: Unknown edit type and unit values are malformed requests")
    void editRejectsUnknownEnumValues() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rose","type":"OTHER","unitOfMeasure":"STEM","sellingPrice":15000,"version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rose","type":"FLOWER","unitOfMeasure":"BOX","sellingPrice":15000,"version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }
}
