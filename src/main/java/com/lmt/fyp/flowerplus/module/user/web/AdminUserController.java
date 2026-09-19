package com.lmt.fyp.flowerplus.module.user.web;

import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import com.lmt.fyp.flowerplus.module.user.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin-only user lookups. The class sits behind the {@code /api/admin/**} URL
 * rule (Admin only) in SecurityConfig, and carries @PreAuthorize as a second
 * layer so the role is checked before any lookup runs — a missing account is a
 * 404, never a 403 that would confirm the id belongs to someone.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        UserProfile profile = userService.getProfile(user);
        return UserResponse.from(user, profile);
    }
}
