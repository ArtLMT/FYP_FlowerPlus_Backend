package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin lists Staff (accounts P5). GET /api/admin/staff is Admin only, returns a
 * real page (Staff have no cap), lists STAFF accounts only, and filters by status.
 */
class AdminStaffListTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    private String adminToken() throws Exception {
        createUser("owner@staff.test", PASSWORD, UserAccountStatus.ACTIVE, UserRole.ADMIN);
        return loginTokens("owner@staff.test", PASSWORD).access();
    }

    private void staff(String email, UserAccountStatus status) {
        createUser(email, PASSWORD, status, UserRole.STAFF);
    }

    private ResultActions list(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/admin/staff" + query)
                .header("Authorization", "Bearer " + token));
    }

    @Test
    @DisplayName("only an Admin may list staff")
    void onlyAdminCanList() throws Exception {
        String admin = adminToken();
        list(admin, "").andExpect(status().isOk());

        createUser("staff@example.com", PASSWORD, UserAccountStatus.ACTIVE, UserRole.STAFF);
        String staffToken = loginTokens("staff@example.com", PASSWORD).access();
        list(staffToken, "").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        createUser("cust@example.com", PASSWORD, UserAccountStatus.ACTIVE);
        String customerToken = loginTokens("cust@example.com", PASSWORD).access();
        list(customerToken, "").andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/staff")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the list holds only STAFF accounts — no customers, no Admin")
    void listsOnlyStaff() throws Exception {
        String admin = adminToken();
        staff("s1@example.com", UserAccountStatus.ACTIVE);
        staff("s2@example.com", UserAccountStatus.ACTIVE);
        createUser("customer@example.com", PASSWORD, UserAccountStatus.ACTIVE);

        list(admin, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].role", everyItem(equalTo("STAFF"))));
    }

    @Test
    @DisplayName("the list really pages")
    void listPages() throws Exception {
        String admin = adminToken();
        staff("a@example.com", UserAccountStatus.ACTIVE);
        staff("b@example.com", UserAccountStatus.ACTIVE);
        staff("c@example.com", UserAccountStatus.ACTIVE);

        list(admin, "?page=0&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        list(admin, "?page=1&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("a status filter narrows the list")
    void statusFilter() throws Exception {
        String admin = adminToken();
        staff("active1@example.com", UserAccountStatus.ACTIVE);
        staff("active2@example.com", UserAccountStatus.ACTIVE);
        staff("banned@example.com", UserAccountStatus.BANNED);

        list(admin, "?status=BANNED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("BANNED"));

        list(admin, "?status=ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status", everyItem(equalTo("ACTIVE"))));
    }

    @Test
    @DisplayName("the page size is clamped to 100")
    void sizeIsClamped() throws Exception {
        String admin = adminToken();

        list(admin, "?size=500")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
