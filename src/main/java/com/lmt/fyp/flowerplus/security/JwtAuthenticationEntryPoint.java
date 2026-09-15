package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Invoked when an unauthenticated user tries to access a protected resource.
 * Writes a structured JSON error response with ErrorCode.UNAUTHENTICATED.
 *
 * <p>Uses Spring's JsonMapper, not a mapper of its own: this runs outside
 * Spring MVC, and a separately built mapper serialized the timestamp as a
 * number while every other error had an ISO-8601 string.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.UNAUTHENTICATED,
                "Authentication is required to access this resource",
                request.getRequestURI());

        response.setStatus(errorResponse.status());
        response.setContentType("application/json");
        response.getWriter().write(jsonMapper.writeValueAsString(errorResponse));
    }
}
