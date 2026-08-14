package com.lmt.fyp.flowerplus.module.auth.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response body returned by both /register and /login endpoints.
 * Contains the signed JWT access token and a refresh token.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @JsonProperty("flowerplus_at")
    private String accessToken;

    @JsonProperty("flowerplus_rt")
    private String refreshToken;
}
