package com.lmt.fyp.flowerplus.module.user.service;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plain unit test — no Spring context, so it runs even where the app can't start. */
class AdminAccountInitializerTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "AdminPassword123";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AdminAccountInitializer initializer(String email, String password) {
        return new AdminAccountInitializer(userRepository, userProfileRepository, passwordEncoder, email, password);
    }

    @Test
    @DisplayName("does nothing when an admin already exists")
    void existingAdminIsLeftAlone() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        initializer("", "").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses to start when there is no admin and the settings are missing")
    void missingSettingsStopStartup() {
        assertThatThrownBy(() -> initializer("", "").run(null))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses a password outside the password policy")
    void weakPasswordStopsStartup() {
        assertThatThrownBy(() -> initializer(EMAIL, "short").run(null))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("never turns an existing account into the admin")
    void existingAccountIsNotPromoted() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> initializer(EMAIL, PASSWORD).run(null))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("creates an active local admin with a hashed password and a profile")
    void createsAdmin() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer(EMAIL, PASSWORD).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertThat(admin.getEmail()).isEqualTo(EMAIL);
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(admin.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(admin.getPassword()).isEqualTo("hashed");
        verify(userProfileRepository).save(any(UserProfile.class));
    }
}
