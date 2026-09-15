package com.lmt.fyp.flowerplus.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: the redirect's error values are ErrorCode names. No Spring context. */
class OAuth2AuthenticationFailureHandlerTest {

    private static final String REDIRECT_URI = "http://localhost:3000/oauth2/redirect";

    private final OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();

    @BeforeEach
    void setUp() {
        // The handler reads its URI through @Value field injection.
        ReflectionTestUtils.setField(handler, "authorizedRedirectUri", REDIRECT_URI);
    }

    @Test
    @DisplayName("a blocked account is sent back with error=ACCOUNT_BLOCKED")
    void blockedAccount() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error(OAuth2AuthenticationFailureHandler.ACCOUNT_BLOCKED)));

        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URI + "?error=ACCOUNT_BLOCKED");
    }

    @Test
    @DisplayName("any other failure is sent back with error=UNAUTHENTICATED")
    void anyOtherFailure() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException("email_not_verified"));

        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URI + "?error=UNAUTHENTICATED");
    }
}
