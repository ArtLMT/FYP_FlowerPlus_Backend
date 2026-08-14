package com.lmt.fyp.flowerplus.security.oauth2;

import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;
import com.lmt.fyp.flowerplus.security.JwtService;
import com.lmt.fyp.flowerplus.security.SecurityUser;
import com.lmt.fyp.flowerplus.module.auth.application.port.out.RefreshTokenPort;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Invoked upon successful OAuth2 authentication. Generates a local JWT token
 * and redirects to the configured frontend callback URL with the token.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenPort refreshTokenPort;

    @Value("${application.security.oauth2.authorized-redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Redirect skipped.");
            return;
        }

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        UserJpaEntity user = oAuth2User.getUser();

        // Generate JWT token and Refresh token for the local user account.
        // The entity is no longer a UserDetails, so wrap it for JWT generation.
        String token = jwtService.generateToken(SecurityUser.fromEntity(user));
        RefreshTokenJpaEntity refreshTokenJpaEntity = refreshTokenPort.create(user);

        // Write cookies
        ResponseCookie accessTokenCookie = ResponseCookie.from("flowerplus_at", token)
                .httpOnly(true)
                .secure(false) // false for local dev
                .path("/")
                .maxAge(86400) // 24 hours
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("flowerplus_rt", refreshTokenJpaEntity.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(604800) // 7 days
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // Redirect WITHOUT tokens in the query string — they are already
        // delivered via the HttpOnly cookies set above. Query params would
        // leak into browser history, server access logs, and Referer headers.
        String targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
