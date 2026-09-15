package com.lmt.fyp.flowerplus.security;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Invoked when an authenticated user tries to access a resource they don't
 * have permission for. Writes a structured JSON error response.
 *
 * <p>Uses Spring's JsonMapper for the same reason as
 * {@link JwtAuthenticationEntryPoint}.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.ACCESS_DENIED,
                "You do not have permission to access this resource",
                request.getRequestURI());

        response.setStatus(errorResponse.status());
        response.setContentType("application/json");
        response.getWriter().write(jsonMapper.writeValueAsString(errorResponse));
    }
}
