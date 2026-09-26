package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialStatus;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers Material status transitions, idempotency, missing resources, and deletion refusal. */
class MaterialStatusTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-03: Staff deactivates and reactivates a material")
    void deactivateAndReactivate() throws Exception {
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
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("T-MAT-03: Repeating either status transition is a no-op")
    void repeatedTransitionsAreNoOps() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(put("/api/manage/materials/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MaterialStatus.DEACTIVATED.name()))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("T-MAT-03: Missing status-transition ids return MATERIAL_NOT_FOUND")
    void missingMaterialCannotTransition() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(put("/api/manage/materials/" + missingId + "/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NOT_FOUND"));

        mockMvc.perform(put("/api/manage/materials/" + missingId + "/reactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MATERIAL_NOT_FOUND"));
    }

    @Test
    @DisplayName("T-MAT-03: Material has no delete endpoint")
    void materialCannotBeDeleted() throws Exception {
        String token = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(token, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);

        mockMvc.perform(delete("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }
}
