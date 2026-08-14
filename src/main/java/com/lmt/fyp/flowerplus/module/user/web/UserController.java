package com.lmt.fyp.flowerplus.module.user.web;

import com.lmt.fyp.flowerplus.module.user.application.port.in.GetUserUseCase;
import com.lmt.fyp.flowerplus.module.user.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * WEB ADAPTER (controller) — OUTSIDE the wall.
 *
 * It depends ONLY on the in-port {@link GetUserUseCase} — an interface —
 * never on the concrete LoginService and never on repositories. Its whole job
 * is translation: receive an HTTP request, call the port, and turn the
 * returned domain model into a UserResponse DTO.
 *
 * This endpoint is NOT whitelisted in SecurityConfig, so it requires a valid
 * JWT — a realistic protected route.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetUserUseCase getUserUseCase;

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable UUID id) {
        return UserResponse.from(getUserUseCase.getUserById(id));
    }
}
