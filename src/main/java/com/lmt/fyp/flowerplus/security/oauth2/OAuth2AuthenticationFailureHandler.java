package com.lmt.fyp.flowerplus.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Invoked when OAuth2 authentication fails.
 *
 * Without this, Spring's default handler redirects to /login?error — a path
 * SecurityConfig does not permit — so the browser lands on a bare 401
 * UNAUTHENTICATED that says nothing about what went wrong.
 *
 * OAuth2 login is a browser redirect flow, so its failures never reach
 * GlobalExceptionHandler (that only sees controller exceptions). Instead we
 * mirror OAuth2AuthenticationSuccessHandler and redirect back to the frontend
 * with a coarse error code the UI can turn into a message.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /** Error code shared with CustomOAuth2UserService; part of the frontend contract. */
    static final String ACCOUNT_BLOCKED = "account_blocked";

    private static final String GENERIC_FAILURE = "authentication_failed";

    @Value("${application.security.oauth2.authorized-redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("error", resolveErrorCode(exception))
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Whitelist rather than pass-through: only codes we deliberately expose
     * reach the URL. Everything else collapses to a generic value so internal
     * detail (provider names, token errors) never leaks into the query string
     * or the browser history. The real cause is already logged by Spring.
     */
    private String resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                && ACCOUNT_BLOCKED.equals(oauth2Exception.getError().getErrorCode())) {
            return ACCOUNT_BLOCKED;
        }
        return GENERIC_FAILURE;
    }
}
