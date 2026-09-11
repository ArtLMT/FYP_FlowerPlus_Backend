package com.lmt.fyp.flowerplus.module.auth.web.dto;

import com.lmt.fyp.flowerplus.common.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Code is required")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = PasswordPolicy.MIN_LENGTH, message = "Password must be at least 8 characters long")
        @Size(max = PasswordPolicy.MAX_LENGTH, message = "Password must be at most 72 characters long")
        String newPassword
) {
}
