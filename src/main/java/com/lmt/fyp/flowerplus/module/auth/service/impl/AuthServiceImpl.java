package com.lmt.fyp.flowerplus.module.auth.service.impl;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.auth.dto.LoginRequest;
import com.lmt.fyp.flowerplus.module.auth.dto.RefreshTokenRequest;
import com.lmt.fyp.flowerplus.module.auth.dto.RegisterRequest;
import com.lmt.fyp.flowerplus.module.auth.dto.AuthResponse;
import com.lmt.fyp.flowerplus.module.auth.entity.RefreshToken;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import com.lmt.fyp.flowerplus.exception.BadRequestException;
import com.lmt.fyp.flowerplus.exception.UnauthorizedException;
import com.lmt.fyp.flowerplus.module.user.repository.UserRepository;
import com.lmt.fyp.flowerplus.module.user.repository.UserProfileRepository;
import com.lmt.fyp.flowerplus.security.JwtService;
import com.lmt.fyp.flowerplus.module.auth.service.AuthService;
import com.lmt.fyp.flowerplus.module.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    // ------------------------------------------------------------------ //
    //  Register
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email is already taken
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Email already registered: " + request.getEmail()
            );
        }

        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                // Always hash passwords — never store plain text
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .status(UserAccountStatus.ACTIVE)
                .provider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);

        // Save user profile details
        UserProfile profile = UserProfile.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .build();
        userProfileRepository.save(profile);

        // Issue tokens immediately so the user is logged in after registration
        String accessToken = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Login
    // ------------------------------------------------------------------ //

    @Override
    public AuthResponse login(LoginRequest request) {
        String loginInput = request.getEmail().trim();

        // AuthenticationManager handles credential verification and throws on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginInput,
                        request.getPassword()
                )
        );

        // If we reach here the credentials are valid
        User user = userRepository.findByEmail(loginInput)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found with email: " + loginInput
                ));

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Refresh Token
    // ------------------------------------------------------------------ //

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        String accessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Logout / Revoke Token
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }
}
