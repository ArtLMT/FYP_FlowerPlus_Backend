package com.lmt.fyp.flowerplus.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload for the POST /api/auth/register endpoint.
 */
@Getter
@Setter
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
}
