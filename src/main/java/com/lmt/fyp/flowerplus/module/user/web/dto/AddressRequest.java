package com.lmt.fyp.flowerplus.module.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank(message = "Receiver name is required")
        @Size(max = 255, message = "Receiver name must be at most 255 characters")
        String receiverName,

        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must be at most 20 characters")
        @Pattern(regexp = "^[0-9+(). -]{6,20}$", message = "Invalid phone format")
        String phone,

        @NotBlank(message = "Address is required")
        String address,

        boolean isDefault
) {
}
