package com.lmt.fyp.flowerplus.module.auth;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * created_by / updated_by on user_account (V6). Writes made with a signed-in
 * user record that user's id; writes with nobody signed in record nothing.
 */
class UserAuditingTest extends AuthIntegrationSupport {

    private static final String PASSWORD = "Password123!";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("self-registration records no author")
    void selfRegistrationHasNoAuthor() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("self@example.com", PASSWORD, "Self"))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("self@example.com").orElseThrow();
        assertThat(user.getCreatedBy()).isNull();
        assertThat(user.getUpdatedBy()).isNull();
    }

    @Test
    @DisplayName("a write made by a signed-in Admin records the Admin in both columns")
    void signedInWriteRecordsTheAuthor() {
        User admin = saveUser("admin-actor@example.com", UserRole.ADMIN, UserAccountStatus.ACTIVE);
        signIn(admin);

        User created = saveUser("created@example.com", UserRole.CUSTOMER, UserAccountStatus.PENDING);

        User reloaded = userRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getCreatedBy()).isEqualTo(admin.getId());
        assertThat(reloaded.getUpdatedBy()).isEqualTo(admin.getId());
    }

    @Test
    @DisplayName("a later write with nobody signed in keeps the last author")
    void anonymousUpdateKeepsTheLastAuthor() {
        User admin = saveUser("admin-actor@example.com", UserRole.ADMIN, UserAccountStatus.ACTIVE);
        signIn(admin);
        User created = saveUser("created@example.com", UserRole.CUSTOMER, UserAccountStatus.PENDING);
        SecurityContextHolder.clearContext();

        // What email verification does: activate the PENDING account, nobody signed in.
        User pending = userRepository.findById(created.getId()).orElseThrow();
        pending.activate();
        userRepository.save(pending);

        User reloaded = userRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(reloaded.getCreatedBy()).isEqualTo(admin.getId());
        assertThat(reloaded.getUpdatedBy()).isEqualTo(admin.getId());
    }

    private User saveUser(String email, UserRole role, UserAccountStatus status) {
        return userRepository.save(User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .status(status)
                .provider(AuthProvider.LOCAL)
                .build());
    }

    private void signIn(User user) {
        SecurityUser principal = SecurityUser.fromEntity(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
