package com.lmt.fyp.flowerplus.controller;

import com.lmt.fyp.flowerplus.dto.request.LoginRequest;
import com.lmt.fyp.flowerplus.dto.request.RegisterRequest;
import com.lmt.fyp.flowerplus.dto.response.AuthResponse;
import com.lmt.fyp.flowerplus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints — no JWT required.
 * All routes here are whitelisted in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Creates a new user account and returns a JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Authenticates an existing user and returns a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}