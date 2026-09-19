package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.security.oauth2.CustomOAuth2UserService;
import com.lmt.fyp.flowerplus.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.lmt.fyp.flowerplus.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central security configuration.
 *
 * Public endpoints : /auth/**  (register & login)
 *                    /oauth2/** and /login/oauth2/** (OAuth2 authentication)
 *                    /swagger-ui/**  and  /v3/api-docs/**  (OpenAPI docs)
 * Staff and Admin  : /api/manage/**
 * Admin only       : /api/admin/**
 * Protected        : everything else requires a valid JWT.
 *
 * Roles and prefixes: docs/modules/user.md. The URL rules are a second layer;
 * management endpoints still carry @PreAuthorize.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // ------------------------------------------------------------------ //
    //  SecurityFilterChain
    // ------------------------------------------------------------------ //

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF disabled. Note this is safe here only because JwtFilter
                // also accepts the token from the flowerplus_at cookie, which
                // browsers attach automatically — so the actual cross-site
                // defense is SameSite=Lax on that cookie, not statelessness.
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints & API docs are public
                        .requestMatchers(
                                "/auth/**",
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Management prefixes; ADMIN passes STAFF through roleHierarchy()
                        .requestMatchers("/api/manage/**").hasRole("STAFF")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Every other request must carry a valid JWT
                        .anyRequest().authenticated()
                )

                // No HTTP sessions — every request is stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider)

                // Exception handling — structured JSON for 401 and 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // Configure OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                // JWT filter runs before Spring's username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ADMIN passes every STAFF check. Nothing is implied for CUSTOMER
     * (Permissions #5 is still open). Static so method security picks it up
     * before this configuration class is created.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("STAFF")
                .build();
    }
}