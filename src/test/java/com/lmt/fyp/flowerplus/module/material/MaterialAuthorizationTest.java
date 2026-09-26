package com.lmt.fyp.flowerplus.module.material;

import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.material.entity.MaterialType;
import com.lmt.fyp.flowerplus.module.material.entity.UnitOfMeasure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers role and authentication requirements across all Material management actions. */
class MaterialAuthorizationTest extends MaterialIntegrationSupport {

    @Test
    @DisplayName("T-MAT-01: Admin can create; Customer and Guest are rejected before validation")
    void createRequiresStaffRole() throws Exception {
        String adminToken = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(post("/api/manage/materials")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("Admin Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000))))
                .andExpect(status().isCreated());

        assertForbidden(post("/api/manage/materials")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthenticated(post("/api/manage/materials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    @Test
    @DisplayName("T-MAT-02: Admin can edit; Customer and Guest are rejected before validation and lookup")
    void editRequiresStaffRole() throws Exception {
        String staffToken = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(
                staffToken, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        String adminToken = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(put("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest(
                                "White Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 16000, 0))))
                .andExpect(status().isOk());

        UUID missingId = UUID.randomUUID();
        assertForbidden(put("/api/manage/materials/" + missingId)
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthenticated(put("/api/manage/materials/" + missingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    @Test
    @DisplayName("T-MAT-03: Admin can change status; Customer and Guest are rejected before validation and lookup")
    void statusChangeRequiresStaffRole() throws Exception {
        String staffToken = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(
                staffToken, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        String adminToken = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(put("/api/manage/materials/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());

        UUID missingId = UUID.randomUUID();
        assertForbidden(put("/api/manage/materials/" + missingId + "/deactivate")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthenticated(put("/api/manage/materials/" + missingId + "/reactivate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    @Test
    @DisplayName("T-MAT-04: Admin can view; Customer and Guest are rejected before lookup")
    void viewRequiresStaffRole() throws Exception {
        String staffToken = tokenFor(STAFF_EMAIL, UserRole.STAFF);
        UUID id = createMaterialViaApi(
                staffToken, "Red Rose", MaterialType.FLOWER, UnitOfMeasure.STEM, 15000);
        String adminToken = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/manage/materials/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        UUID missingId = UUID.randomUUID();
        assertForbidden(get("/api/manage/materials/" + missingId)
                .header("Authorization", "Bearer " + customerToken));
        assertUnauthenticated(get("/api/manage/materials/" + missingId));
    }

    @Test
    @DisplayName("T-MAT-04/07: Admin can list; Customer and Guest are rejected before query validation")
    void listRequiresStaffRole() throws Exception {
        String adminToken = tokenFor(ADMIN_EMAIL, UserRole.ADMIN);
        String customerToken = tokenFor(CUSTOMER_EMAIL, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/manage/materials")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertForbidden(get("/api/manage/materials")
                .param("page", "invalid")
                .header("Authorization", "Bearer " + customerToken));
        assertUnauthenticated(get("/api/manage/materials")
                .param("page", "invalid"));
    }

    private void assertForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    private void assertUnauthenticated(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
