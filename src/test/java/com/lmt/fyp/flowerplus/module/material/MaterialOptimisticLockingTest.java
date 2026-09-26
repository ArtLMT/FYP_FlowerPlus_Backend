package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.dto.UpdateMaterialRequest;
import com.lmt.fyp.flowerplus.module.material.entity.Material;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialOptimisticLockingTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-06: Material reads expose the initial version")
    void materialReadIncludesVersion() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(get("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("T-MAT-06: Edit requires a non-negative version")
    void updateRequiresValidVersion() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"White Rose","type":"FLOWER","unitOfMeasure":"STEM","sellingPrice":16000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[?(@.field == 'version')]").exists());

        UpdateMaterialRequest negativeVersion = new UpdateMaterialRequest(
                "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, BigDecimal.valueOf(16000), -1L);
        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(negativeVersion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("T-MAT-06: Status changes require a non-negative version")
    void statusChangesRequireValidVersion() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields[?(@.field == 'version')]").exists());

        mockMvc.perform(put("/api/manage/materials/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("T-MAT-06: A stale edit is rejected without changing the material")
    void staleEditIsRejected() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        UpdateMaterialRequest firstEdit = new UpdateMaterialRequest(
                "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, BigDecimal.valueOf(16000), 0L);
        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(firstEdit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        UpdateMaterialRequest staleEdit = new UpdateMaterialRequest(
                "Yellow Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, BigDecimal.valueOf(17000), 0L);
        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(staleEdit)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));

        mockMvc.perform(get("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("White Rose"))
                .andExpect(jsonPath("$.sellingPrice").value(16000))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("T-MAT-06: Status changes reject stale versions and accept the latest version")
    void statusChangeUsesVersion() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.DEACTIVATED.name()))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/manage/materials/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));

        mockMvc.perform(get("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.DEACTIVATED.name()))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/manage/materials/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("T-MAT-06: Authorization happens before version-body validation")
    void statusVersionIsProtectedByRoleFirst() throws Exception {
        String staffToken = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(staffToken, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("T-MAT-06: JPA rejects a detached entity with a stale version")
    void databaseOptimisticLockRejectsSeparateTransactionRace() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        Material firstSnapshot = materialRepository.findById(id).orElseThrow();
        Material secondSnapshot = materialRepository.findById(id).orElseThrow();

        firstSnapshot.update("White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, BigDecimal.valueOf(16000));
        materialRepository.saveAndFlush(firstSnapshot);

        secondSnapshot.update("Yellow Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, BigDecimal.valueOf(17000));
        assertThrows(OptimisticLockingFailureException.class,
                () -> materialRepository.saveAndFlush(secondSnapshot));
    }
}
