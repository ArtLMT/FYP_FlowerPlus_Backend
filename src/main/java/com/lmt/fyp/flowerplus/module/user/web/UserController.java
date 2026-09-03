package com.lmt.fyp.flowerplus.module.user.web;

import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import com.lmt.fyp.flowerplus.module.user.web.dto.UserResponse;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Protected user endpoints — requires a valid JWT (not whitelisted in
 * SecurityConfig). Maps the service's entities into a wire-shaped DTO.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal SecurityUser principal) {
        User user = userService.getByEmail(principal.getUsername());
        UserProfile profile = userService.getProfile(user);
        return UserResponse.from(user, profile);
    }

    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or returnObject.email == authentication.name")
    public UserResponse getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        UserProfile profile = userService.getProfile(user);
        return UserResponse.from(user, profile);
    }
}
