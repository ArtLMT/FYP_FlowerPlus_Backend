package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorResponse;
import com.lmt.fyp.flowerplus.exception.GlobalExceptionHandler;
import com.lmt.fyp.flowerplus.module.material.dto.CreateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.dto.UpdateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagementMaterialCrudTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-01: Staff can create a new active material")
    void createMaterialSuccess() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Red Rose"))
                .andExpect(jsonPath("$.status").value(MaterialStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("T-MAT-01: Creating a material with an existing name returns 409")
    void createMaterialDuplicateName() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("  red rose  ", MaterialType.DECORATION, UnitOfMeasure.PIECE, 20000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NAME_EXISTS"));
    }

    @Test
    @DisplayName("T-MAT-05: Material names are trimmed before they are saved")
    void createMaterialTrimsNameBeforeSaving() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("  Red Rose  ", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    @DisplayName("T-MAT-05: Fractional selling prices are rejected for create and edit")
    void rejectFractionalSellingPrice() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        CreateMaterialRequest fractionalCreate = new CreateMaterialRequest(
                "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, new BigDecimal("15000.5"));

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(fractionalCreate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        UUID id = createMaterialViaApi(token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        UpdateMaterialRequest fractionalUpdate = new UpdateMaterialRequest(
                "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, new BigDecimal("15000.5"), 0L);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(fractionalUpdate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("T-MAT-05: Database constraints reject invalid material values")
    void databaseConstraintsRejectInvalidMaterialValues() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'Invalid price', 'FLOWER', 'STEM', 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'Invalid type', 'OTHER', 'STEM', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'Invalid unit', 'FLOWER', 'BOX', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'Invalid status', 'FLOWER', 'STEM', 1, 'REMOVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """));

        jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'Unique material', 'FLOWER', 'STEM', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO material (id, name, type, unit_of_measure, selling_price, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'unique material', 'FLOWER', 'STEM', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """));
    }

    @Test
    @DisplayName("T-MAT-05: A material-name unique-index race is returned as the documented conflict")
    void materialNameUniqueConstraintIsMappedToConflict() {
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate material name", null, "idx_material_unique_name");
        DataIntegrityViolationException databaseFailure = new DataIntegrityViolationException(
                "duplicate material name", constraintViolation);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/manage/materials");

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleDataIntegrityViolation(databaseFailure, request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals(ErrorCode.MATERIAL_NAME_EXISTS.name(), response.getBody().errorCode());
    }

    @Test
    @DisplayName("T-MAT-02: Staff can edit a material including deactivated ones")
    void editMaterialSuccess() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        UpdateMaterialRequest updateRequest = new UpdateMaterialRequest(
                "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, BigDecimal.valueOf(16000), 0L
        );

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("White Rose"))
                .andExpect(jsonPath("$.sellingPrice").value(16000));
    }

    @Test
    @DisplayName("T-MAT-03: Staff can deactivate and reactivate materials")
    void deactivateAndReactivate() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        // Deactivate
        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.DEACTIVATED.name()))
                .andExpect(jsonPath("$.version").value(1));

        // Reactivate
        mockMvc.perform(put("/api/manage/materials/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("T-MAT-04: Staff can view a single material")
    void getSingleMaterial() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(get("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Red Rose"));
    }

    @Test
    @DisplayName("T-MAT-04: Staff can list materials with search, filter, and pagination")
    void listMaterials() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        createMaterialViaApi(token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        createMaterialViaApi(token, "Ribbon", MaterialType.DECORATION, UnitOfMeasure.METRE, 5000);

        // List all flowers with "rose" in name
        mockMvc.perform(get("/api/manage/materials?search=rose&type=FLOWER&page=0&size=20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("T-MAT-07: Name search treats SQL wildcard characters literally")
    void listMaterialsSearchesWildcardCharactersLiterally() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "50% Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        createMaterialViaApi(token, "Rose_Stem", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 17000);

        mockMvc.perform(get("/api/manage/materials")
                        .param("search", "%")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("50% Rose"));

        mockMvc.perform(get("/api/manage/materials")
                        .param("search", "_")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Rose_Stem"));
    }

    @Test
    @DisplayName("T-MAT-07: Approved sort options select fixed fields and directions")
    void listMaterialsUsesApprovedSortOptions() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 30000);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 10000);

        mockMvc.perform(get("/api/manage/materials?sort=NAME_DESC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("White Rose"));

        mockMvc.perform(get("/api/manage/materials?sort=PRICE_ASC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Red Rose"));
    }

    @Test
    @DisplayName("T-MAT-04: Invalid page size > 100 returns validation failure")
    void listMaterialsPageSizeExceeded() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(get("/api/manage/materials?size=101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("T-MAT-07: Negative pages and non-positive sizes return validation failure")
    void listMaterialsRejectsInvalidPageBounds() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);

        mockMvc.perform(get("/api/manage/materials?page=-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/manage/materials?size=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("T-MAT-07: Staff and Admin can list materials; Customer and Guest cannot")
    void listMaterialsRequiresStaffRole() throws Exception {
        String adminToken = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/manage/materials")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manage/materials")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manage/materials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T-MAT-07: Unsupported sort uses the default name ascending order")
    void listMaterialsUnsupportedSortUsesDefault() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        createMaterialViaApi(token, "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(get("/api/manage/materials?sort=string")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Red Rose"));
    }
}
