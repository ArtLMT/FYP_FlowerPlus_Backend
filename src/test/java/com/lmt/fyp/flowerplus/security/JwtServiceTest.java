package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link JwtService}. It runs without a Spring context: the two
 * {@code @Value} fields are set directly with {@link ReflectionTestUtils}. The
 * secret is a Base64 string decoding to 48 bytes — above the 256-bit minimum
 * HS256 requires.
 */
class JwtServiceTest {

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private JwtService jwtService;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L); // 1 hour
        user = new SecurityUser("user@example.com", "pw", UserRole.CUSTOMER, UserAccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("a freshly generated token validates and carries the username")
    void generatedTokenValidatesAndCarriesSubject() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("a token is not valid for a different user")
    void tokenNotValidForDifferentUser() {
        String token = jwtService.generateToken(user);

        UserDetails other =
                new SecurityUser("someone.else@example.com", "pw", UserRole.CUSTOMER, UserAccountStatus.ACTIVE);

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("garbage is rejected, not thrown")
    void garbageTokenIsRejected() {
        assertThat(jwtService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("an expired token fails validation")
    void expiredTokenIsRejected() {
        // Negative lifetime → the token is already expired the moment it is built.
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1_000L);
        String expired = jwtService.generateToken(user);

        assertThat(jwtService.validateToken(expired)).isFalse();
    }
}
