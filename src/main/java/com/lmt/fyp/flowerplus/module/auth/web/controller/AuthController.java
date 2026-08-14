package com.lmt.fyp.flowerplus.module.auth.web.controller;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.web.dto.LoginRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RefreshTokenRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.auth.web.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.LoginUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.LogoutUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.RefreshTokenUseCase;
import com.lmt.fyp.flowerplus.module.auth.application.port.in.RegisterUseCase;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    /**
     * POST /api/auth/register
     * Creates a new user account and returns access + refresh tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = registerUseCase.register(request);
        setTokenCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/auth/login
     * Authenticates an existing user and returns access + refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = loginUseCase.login(request);
        setTokenCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/auth/refresh
     * Validates a refresh token and returns a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = "flowerplus_rt", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String refreshToken = null;
        if (refreshTokenCookie != null) {
            refreshToken = refreshTokenCookie;
        } else if (requestBody != null) {
            refreshToken = requestBody.getRefreshToken();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token is missing");
        }

        AuthResponse authResponse = refreshTokenUseCase.refreshToken(refreshToken);
        setTokenCookies(response, authResponse);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * POST /api/auth/logout
     * Revokes/invalidates a refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = "flowerplus_rt", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String refreshToken = null;
        if (refreshTokenCookie != null) {
            refreshToken = refreshTokenCookie;
        } else if (requestBody != null) {
            refreshToken = requestBody.getRefreshToken();
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            logoutUseCase.logout(refreshToken);
        }

        clearTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    private void setTokenCookies(HttpServletResponse response, AuthResponse authResponse) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("flowerplus_at", authResponse.getAccessToken())
                .httpOnly(true)
                .secure(false) // Set to true in production/HTTPS
                .path("/")
                .maxAge(86400) // 24 hours
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("flowerplus_rt", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(604800) // 7 days
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie deleteAccess = ResponseCookie.from("flowerplus_at", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie deleteRefresh = ResponseCookie.from("flowerplus_rt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());
    }
}