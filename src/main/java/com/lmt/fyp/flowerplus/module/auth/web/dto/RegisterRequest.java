package com.lmt.fyp.flowerplus.module.auth.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for the POST /api/auth/register endpoint.
 */
@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, message = "Full name must be at least 2 characters long")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
