package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.web.dto.CreateStaffRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin creates and deactivates Staff (accounts P3 and P4). A new Staff account
 * is ACTIVE with a random password the Admin never sees; the person sets their
 * own through the emailed code, delivered as a welcome mail but verified by the
 * ordinary reset endpoint. Deactivation is BANNED, reversible, and reuses every
 * existing blocked-account check.
 */
class StaffAccountTest extends AuthIntegrationSupport {

    private static final String ADMIN_PASSWORD = "AdminPass123!";

    private record Admin(User user, String token) {
    }

    private Admin admin() throws Exception {
        User user = createUser("owner@staff.test", ADMIN_PASSWORD, UserAccountStatus.ACTIVE, UserRole.ADMIN);
        return new Admin(user, loginTokens("owner@staff.test", ADMIN_PASSWORD).access());
    }

    private ResultActions createStaff(String token, CreateStaffRequest body) throws Exception {
        return mockMvc.perform(post("/api/admin/staff")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    // ---------------------------------------------------------------- P3 --//

    @Test
    @DisplayName("Admin creates an ACTIVE STAFF account with a lowercased email and a Location header")
    void adminCreatesStaff() throws Exception {
        String token = admin().token();

        createStaff(token, new CreateStaffRequest("New.Staff@Example.com", "New Staff"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/admin/users/")))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").value("new.staff@example.com"))
                .andExpect(jsonPath("$.fullName").value("New Staff"));
    }

    @Test
    @DisplayName("the emailed code sets the first password, then the Staff member can sign in")
    void staffSetsPasswordAndSignsIn() throws Exception {
        String token = admin().token();
        createStaff(token, new CreateStaffRequest("hire@example.com", "Hired Person"))
                .andExpect(status().isCreated());

        String code = awaitOtp(1);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "hire@example.com", "code", code, "newPassword", "StaffPass123!"))))
                .andExpect(status().isNoContent());

        assertThat(loginTokens("hire@example.com", "StaffPass123!").access()).isNotBlank();
    }

    @Test
    @DisplayName("a PENDING account on that email is adopted as STAFF and its old password is discarded")
    void pendingAccountIsAdopted() throws Exception {
        createUser("pending@example.com", "OldPass123!", UserAccountStatus.PENDING);
        String token = admin().token();

        createStaff(token, new CreateStaffRequest("pending@example.com", "Adopted Staff"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // The never-proven password is gone: the old one no longer authenticates.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(loginRequest("pending@example.com", "OldPass123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        assertThat(userRepository.findByEmail("pending@example.com").orElseThrow().getRole())
                .isEqualTo(UserRole.STAFF);
    }

    @Test
    @DisplayName("an ACTIVE customer email is 409 and stays a CUSTOMER")
    void activeEmailConflicts() throws Exception {
        createUser("taken@example.com", "Password123!", UserAccountStatus.ACTIVE);
        String token = admin().token();

        createStaff(token, new CreateStaffRequest("taken@example.com", "Should Fail"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));

        assertThat(userRepository.findByEmail("taken@example.com").orElseThrow().getRole())
                .isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("a BANNED email is 409, so a banned address cannot be reused as staff")
    void bannedEmailConflicts() throws Exception {
        createUser("banned@example.com", "Password123!", UserAccountStatus.BANNED);
        String token = admin().token();

        createStaff(token, new CreateStaffRequest("banned@example.com", "Should Fail"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("a Staff caller cannot create staff")
    void staffCannotCreate() throws Exception {
        createUser("staff@example.com", "Password123!", UserAccountStatus.ACTIVE, UserRole.STAFF);
        String staffToken = loginTokens("staff@example.com", "Password123!").access();

        createStaff(staffToken, new CreateStaffRequest("x@example.com", "X"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("a customer caller cannot create staff")
    void customerCannotCreate() throws Exception {
        createUser("cust@example.com", "Password123!", UserAccountStatus.ACTIVE);
        String customerToken = loginTokens("cust@example.com", "Password123!").access();

        createStaff(customerToken, new CreateStaffRequest("x@example.com", "X"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("no token is unauthorized")
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateStaffRequest("x@example.com", "X"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("a body with no full name is a validation failure")
    void invalidBodyIsRejected() throws Exception {
        String token = admin().token();

        createStaff(token, new CreateStaffRequest("x@example.com", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    // ---------------------------------------------------------------- P4 --//

    private User activeStaff(String email) {
        return createUser(email, "StaffPass123!", UserAccountStatus.ACTIVE, UserRole.STAFF);
    }

    private ResultActions deactivate(String token, UUID id) throws Exception {
        return mockMvc.perform(put("/api/admin/staff/" + id + "/deactivate")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions reactivate(String token, UUID id) throws Exception {
        return mockMvc.perform(put("/api/admin/staff/" + id + "/reactivate")
                .header("Authorization", "Bearer " + token));
    }

    @Test
    @DisplayName("a deactivated Staff account is blocked at once and cannot log in")
    void deactivatedStaffIsBlocked() throws Exception {
        User staff = activeStaff("s1@example.com");
        String staffToken = loginTokens("s1@example.com", "StaffPass123!").access();
        String adminToken = admin().token();

        deactivate(adminToken, staff.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"));

        // The already-issued access token stops working on the next request.
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isUnauthorized());

        // And login is refused as a blocked account.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(loginRequest("s1@example.com", "StaffPass123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_BLOCKED"));
    }

    @Test
    @DisplayName("reactivating a Staff account lets them sign in again")
    void reactivatedStaffCanSignIn() throws Exception {
        User staff = activeStaff("s2@example.com");
        String adminToken = admin().token();

        deactivate(adminToken, staff.getId()).andExpect(status().isOk());
        reactivate(adminToken, staff.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(loginTokens("s2@example.com", "StaffPass123!").access()).isNotBlank();
    }

    @Test
    @DisplayName("repeating deactivate or reactivate is a 200 no-op, never a 500")
    void repeatingIsIdempotent() throws Exception {
        User staff = activeStaff("s3@example.com");
        String adminToken = admin().token();

        deactivate(adminToken, staff.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BANNED"));
        deactivate(adminToken, staff.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BANNED"));
        reactivate(adminToken, staff.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        reactivate(adminToken, staff.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("a customer id is 404 and the customer is untouched")
    void customerIdIsNotFound() throws Exception {
        User customer = createUser("c@example.com", "Password123!", UserAccountStatus.ACTIVE);
        String adminToken = admin().token();

        deactivate(adminToken, customer.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));

        assertThat(userRepository.findById(customer.getId()).orElseThrow().getStatus())
                .isEqualTo(UserAccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("the Admin's own id is 404 — deactivation targets STAFF only")
    void adminIdIsNotFound() throws Exception {
        Admin admin = admin();

        deactivate(admin.token(), admin.user().getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("a Staff caller cannot deactivate")
    void staffCannotDeactivate() throws Exception {
        User target = activeStaff("target@example.com");
        createUser("actor@example.com", "Password123!", UserAccountStatus.ACTIVE, UserRole.STAFF);
        String staffToken = loginTokens("actor@example.com", "Password123!").access();

        deactivate(staffToken, target.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
