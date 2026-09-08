package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.auth.exception.EmailUsedException;
import com.lmt.fyp.flowerplus.module.auth.service.AuthService;
import com.lmt.fyp.flowerplus.module.auth.service.OtpService;
import com.lmt.fyp.flowerplus.module.auth.service.RefreshTokenService;
import com.lmt.fyp.flowerplus.module.auth.service.TokenPair;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.service.UserService;
import com.lmt.fyp.flowerplus.security.JwtService;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Transactional so the OTP event, which fires AFTER_COMMIT, is never sent
    // for a registration that rolled back.
    @Override
    @Transactional
    public void register(String email, String rawPassword, String fullName) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        Optional<User> existing = userService.findByEmail(normalizedEmail);

        if (existing.isPresent()) {
            User user = existing.get();

            // PENDING is the only status that may be re-registered: the account was never verified, so a fresh code and password overwrite it.
            if (user.getStatus() == UserAccountStatus.PENDING) {
                userService.resetPendingAccount(user, passwordEncoder.encode(rawPassword), fullName);
                otpService.issueOTP(normalizedEmail);
                return;
            }

            // ACTIVE, SUSPENDED or BANNED: an account already owns this email.
            // All three answer with the same 409 so a caller cannot tell a
            // banned address apart from an ordinary registered one. Previously
            // only ACTIVE was handled; the others fell through to a duplicate
            // INSERT and surfaced as a 500.
            throw new EmailUsedException("Email already registered");
        }

        userService.createPendingAccount(normalizedEmail, passwordEncoder.encode(rawPassword), fullName);

        otpService.issueOTP(normalizedEmail);
    }

    @Override
    public TokenPair login(String email, String rawPassword) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, rawPassword)
        );

        User user = userService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.USER_NOT_FOUND, "User not found with email: " + normalizedEmail));

        return new TokenPair(
                jwtService.generateToken(SecurityUser.fromEntity(user)),
                refreshTokenService.create(user).getToken());
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    // Not @Transactional: rotate() owns its own transaction (that is what makes
    // its noRollbackFor reuse-cleanup durable). The user it returns is eagerly
    // loaded, so reading status/identity here needs no open session.
    @Override
    public TokenPair refresh(String refreshToken) {
        RefreshToken rotated = refreshTokenService.rotate(refreshToken);

        // A revoked account status must invalidate the session immediately,
        // not wait out the refresh token's remaining lifetime. (Edge: a blocked
        // account's token has already been rotated by this point — the new row
        // is never returned and is swept as a tombstone; harmless.)
        User user = rotated.getUser();
        if (SecurityUser.isAuthBlocked(user.getStatus())) {
            throw new UnauthorizedException(
                    ErrorCode.REFRESH_TOKEN_INVALID, "Account is not permitted to refresh");
        }

        return new TokenPair(
                jwtService.generateToken(SecurityUser.fromEntity(user)),
                rotated.getToken());
    }
}
