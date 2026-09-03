package com.lmt.fyp.flowerplus.module.user.service.impl;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.exception.UserNotFoundException;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

// Read-only by default; every write below overrides it explicitly.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public UserProfile getProfile(User user) {
        return userProfileRepository.findByUser(user).orElse(null);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public User createPendingAccount(String email, String hashedPassword, String fullName) {
        User user = User.builder()
                .username(email)
                .email(email)
                .password(hashedPassword)
                .role(UserRole.CUSTOMER)
                .status(UserAccountStatus.PENDING)
                .provider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);

        userProfileRepository.save(UserProfile.builder()
                .user(savedUser)
                .fullName(fullName)
                .build());

        return savedUser;
    }

    @Override
    @Transactional
    public void resetPendingAccount(User user, String hashedPassword, String fullName) {
        user.setPassword(hashedPassword);
        userProfileRepository.findByUser(user)
                .ifPresent(profile -> profile.setFullName(fullName));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activate(User user) {
        user.setStatus(UserAccountStatus.ACTIVE);
    }
}
