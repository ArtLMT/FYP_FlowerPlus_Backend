package com.lmt.fyp.flowerplus.module.user.service.impl;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.exception.ApiException;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.event.StaffAccountCreatedEvent;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import com.lmt.fyp.flowerplus.module.user.service.StaffAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffAccountServiceImpl implements StaffAccountService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public User createStaff(String email, String fullName) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        // The Admin never sees this. It only fills the not-null password column
        // until the real one is set through the emailed code. A fresh random one
        // also replaces any adopted PENDING account's never-proven password.
        String randomHash = passwordEncoder.encode(randomPassword());

        User staff = userRepository.findByEmail(normalizedEmail)
                .map(existing -> adoptOrReject(existing, randomHash, fullName))
                .orElseGet(() -> createFreshStaff(normalizedEmail, randomHash, fullName));

        // AFTER_COMMIT so the invitation code is issued only once the account is
        // durable; the user module never learns how the code is delivered.
        eventPublisher.publishEvent(new StaffAccountCreatedEvent(normalizedEmail));
        return staff;
    }

    private User adoptOrReject(User existing, String randomHash, String fullName) {
        // An ACTIVE, SUSPENDED or BANNED email already belongs to someone; only a
        // never-verified PENDING account can be taken over (a squatter can't block
        // a hire). Same 409 as registration, so nothing about the email leaks.
        if (existing.getStatus() != UserAccountStatus.PENDING) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email already registered");
        }
        existing.adoptAsStaff(randomHash);
        setProfileName(existing, fullName);
        return existing;
    }

    private User createFreshStaff(String normalizedEmail, String randomHash, String fullName) {
        User staff = userRepository.save(User.builder()
                .username(normalizedEmail)
                .email(normalizedEmail)
                .password(randomHash)
                .role(UserRole.STAFF)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(staff)
                .fullName(fullName)
                .build());
        return staff;
    }

    // The adopted account already has a profile (registration created one); the
    // name the Admin entered is the staff member's, so it wins. A missing profile
    // is only defensive — a registered PENDING account always has one.
    private void setProfileName(User user, String fullName) {
        userProfileRepository.findByUser(user)
                .ifPresentOrElse(
                        profile -> profile.setFullName(fullName),
                        () -> userProfileRepository.save(UserProfile.builder()
                                .user(user)
                                .fullName(fullName)
                                .build()));
    }

    @Override
    @Transactional
    public User deactivate(UUID id) {
        User staff = requireStaff(id);
        if (staff.getStatus() != UserAccountStatus.BANNED) {
            staff.ban();
        }
        return staff;
    }

    @Override
    @Transactional
    public User reactivate(UUID id) {
        User staff = requireStaff(id);
        if (staff.getStatus() != UserAccountStatus.ACTIVE) {
            staff.unban();
        }
        return staff;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listStaff(UserAccountStatus status, Pageable pageable) {
        return status == null
                ? userRepository.findByRole(UserRole.STAFF, pageable)
                : userRepository.findByRoleAndStatus(UserRole.STAFF, status, pageable);
    }

    // Role-scoped: a customer's or the Admin's id is 404, so these endpoints act
    // on Staff accounts only and never confirm a non-Staff id exists.
    private User requireStaff(UUID id) {
        return userRepository.findByIdAndRole(id, UserRole.STAFF)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "Staff not found with id: " + id));
    }

    private static String randomPassword() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
