package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.util.EmailNormalizer;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.auth.exception.EmailUsedException;
import com.lmt.fyp.flowerplus.module.auth.service.AuthService;
import com.lmt.fyp.flowerplus.module.auth.service.OtpPurpose;
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

    // Deliberately NOT @Transactional. createPendingAccount runs in its own
    // transaction and commits before the OTP is issued, so a failed account
    // write can never leave an orphan code or a burnt cooldown in Redis (D5) —
    // the OTP side effect only happens once the account it depends on is
    // durable. The OTP email still fires only for a real account: issueOTP is
    // reached only after the account commits.
    @Override
    public void register(String email, String rawPassword, String fullName) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        Optional<User> existing = userService.findByEmail(normalizedEmail);

        if (existing.isPresent()) {
            User user = existing.get();

            // PENDING: never verified. Re-issue the code only — the password is
            // NOT overwritten. Ownership is unproven at registration time, so
            // letting a re-registration reset the password would let a stranger
            // hijack the pending account (N11). Recovery of a genuinely stuck
            // account is the job of password reset, not re-registration.
            if (user.getStatus() == UserAccountStatus.PENDING) {
                otpService.issueOTP(OtpPurpose.REGISTRATION, normalizedEmail);
                return;
            }

            // ACTIVE, SUSPENDED or BANNED: an account already owns this email.
            // All three answer with the same 409 so a caller cannot tell a
            // banned address apart from an ordinary registered one.
            throw new EmailUsedException("Email already registered");
        }

        userService.createPendingAccount(normalizedEmail, passwordEncoder.encode(rawPassword), fullName);

        otpService.issueOTP(OtpPurpose.REGISTRATION, normalizedEmail);
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
