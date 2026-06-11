package com.lmt.fyp.flowerplus.service.impl;

import com.lmt.fyp.flowerplus.dto.request.LoginRequest;
import com.lmt.fyp.flowerplus.dto.request.RegisterRequest;
import com.lmt.fyp.flowerplus.dto.response.AuthResponse;
import com.lmt.fyp.flowerplus.entity.User;
import com.lmt.fyp.flowerplus.repository.UserRepository;
import com.lmt.fyp.flowerplus.security.JwtService;
import com.lmt.fyp.flowerplus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ------------------------------------------------------------------ //
    //  Register
    // ------------------------------------------------------------------ //

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Check if email is already taken
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                // Always hash passwords — never store plain text
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        // Issue a token immediately so the user is logged in after registration
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Login
    // ------------------------------------------------------------------ //

    @Override
    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager handles credential verification and throws on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we reach here the credentials are valid
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .build();
    }
}
