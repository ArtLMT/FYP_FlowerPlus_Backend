package com.lmt.fyp.flowerplus.module.user.service;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.PasswordPolicy;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the single Admin account at startup when none exists, from
 * ADMIN_EMAIL / ADMIN_PASSWORD. Runs on every startup but only acts when there
 * is no admin. It refuses to start rather than run without one, and never turns
 * an existing account into the admin.
 */
@Slf4j
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            @Value("${application.admin.email:}") String adminEmail,
            @Value("${application.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "No admin account exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set.");
        }
        if (!PasswordPolicy.isSatisfiedBy(adminPassword)) {
            throw new IllegalStateException("ADMIN_PASSWORD must be " + PasswordPolicy.MIN_LENGTH
                    + "–" + PasswordPolicy.MAX_LENGTH + " characters long.");
        }

        String email = EmailNormalizer.normalize(adminEmail);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("ADMIN_EMAIL " + email
                    + " already belongs to an account; an existing account is never made admin.");
        }

        User admin = userRepository.save(User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(admin)
                .fullName("Administrator")
                .build());

        log.info("Created the admin account {}", email);
    }
}
