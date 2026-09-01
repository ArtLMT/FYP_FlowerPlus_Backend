package com.lmt.fyp.flowerplus.module.auth.web.controller;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.service.AuthService;
import com.lmt.fyp.flowerplus.module.auth.service.EmailVerificationService;
import com.lmt.fyp.flowerplus.module.auth.service.TokenPair;
import com.lmt.fyp.flowerplus.module.auth.web.dto.*;
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

    private static final String ACCESS_TOKEN_COOKIE = "flowerplus_at";
    private static final String REFRESH_TOKEN_COOKIE = "flowerplus_rt";
    private static final long ACCESS_TOKEN_MAX_AGE = 86400;   // 24 hours
    private static final long REFRESH_TOKEN_MAX_AGE = 604800; // 7 days

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /**
     * POST /api/auth/register
     * Creates a pending account and mails a verification code. Deliberately
     * returns no tokens — the account is not usable until it is verified.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getPassword(), request.getFullName());

        return ResponseEntity.ok(RegisterResponse.builder()
                .message("Please check your email")
                .build());
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
        TokenPair tokens = authService.login(request.getEmail(), request.getPassword());
        return respondWithTokens(tokens, response);
    }

    /**
     * POST /api/auth/refresh
     * Validates a refresh token and returns a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(requestBody, refreshTokenCookie);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token is missing");
        }

        TokenPair tokens = authService.refresh(refreshToken);
        return respondWithTokens(tokens, response);
    }

    /**
     * POST /api/auth/logout
     * Revokes the refresh token and clears both cookies.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(requestBody, refreshTokenCookie);

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        clearTokenCookies(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/resend-otp
     * Re-issues a verification code to a still-pending account. Always 204:
     * the response is identical whether or not a code was sent, so it cannot be
     * used to tell which emails are registered.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        emailVerificationService.resend(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse response
    ) {
        TokenPair tokens = emailVerificationService.verifyEmail(request.getEmail(), request.getCode());
        return respondWithTokens(tokens, response);
    }

    /**
     * The refresh token may arrive in the cookie or the body; the cookie wins.
     * Shared by refresh and logout, which resolve it identically.
     */
    private String resolveRefreshToken(RefreshTokenRequest requestBody, String refreshTokenCookie) {
        if (refreshTokenCookie != null) {
            return refreshTokenCookie;
        }
        return requestBody != null ? requestBody.getRefreshToken() : null;
    }

    private ResponseEntity<AuthResponse> respondWithTokens(TokenPair tokens, HttpServletResponse response) {
        setTokenCookies(response, tokens);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .build());
    }

    private void setTokenCookies(HttpServletResponse response, TokenPair tokens) {
        addCookie(response, ACCESS_TOKEN_COOKIE, tokens.accessToken(), ACCESS_TOKEN_MAX_AGE);
        addCookie(response, REFRESH_TOKEN_COOKIE, tokens.refreshToken(), REFRESH_TOKEN_MAX_AGE);
    }

    private void clearTokenCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // Set to true in production/HTTPS
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
