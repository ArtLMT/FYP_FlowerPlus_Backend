package com.lmt.fyp.flowerplus.module.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/admin/staff. No password: a new Staff member sets their
 * own through the emailed code, so the Admin never chooses or sees one.
 */
public record CreateStaffRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, message = "Full name must be at least 2 characters long")
        String fullName
) {
}
