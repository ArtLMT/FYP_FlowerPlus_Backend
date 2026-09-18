package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The two management prefixes, before any endpoint exists under them.
 * 404 means "passed security, no endpoint yet"; 403 means the URL rule stopped it.
 */
class ManagementPrefixTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    private String tokenFor(UserRole role) throws Exception {
        String email = role.name().toLowerCase() + "-caller@example.com";
        createUser(email, PASSWORD, UserAccountStatus.ACTIVE, role);
        return loginTokens(email, PASSWORD).access();
    }

    private ResultActions getAs(String path, UserRole role) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + tokenFor(role)));
    }

    @Test
    @DisplayName("/api/manage: Staff passes")
    void manageAllowsStaff() throws Exception {
        getAs("/api/manage/x", UserRole.STAFF).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/api/manage: Admin passes, which proves the hierarchy is wired")
    void manageAllowsAdmin() throws Exception {
        getAs("/api/manage/x", UserRole.ADMIN).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/api/manage: a customer is 403")
    void manageRejectsCustomer() throws Exception {
        getAs("/api/manage/x", UserRole.CUSTOMER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("/api/manage: no token is 401")
    void manageWithoutTokenIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/manage/x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("/api/admin: Admin passes")
    void adminAllowsAdmin() throws Exception {
        getAs("/api/admin/x", UserRole.ADMIN).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/api/admin: Staff is 403")
    void adminRejectsStaff() throws Exception {
        getAs("/api/admin/x", UserRole.STAFF)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("/api/admin: a customer is 403")
    void adminRejectsCustomer() throws Exception {
        getAs("/api/admin/x", UserRole.CUSTOMER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
