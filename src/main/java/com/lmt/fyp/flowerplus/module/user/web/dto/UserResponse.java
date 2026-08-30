package com.lmt.fyp.flowerplus.module.user.web.dto;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.entity.User;
import com.lmt.fyp.flowerplus.module.user.entity.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * RESPONSE DTO (web layer) — the flat JSON shape returned to the client.
 * The controller flattens the user account and its profile into this shape.
 */
@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private UserRole role;
    private UserAccountStatus status;
    private AuthProvider provider;
    private String fullName;
    private String phone;
    private String avatar;

    /** Translate the account entity (+ its profile) into the wire shape. */
    public static UserResponse from(User user, UserProfile profile) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .provider(user.getProvider())
                .fullName(profile != null ? profile.getFullName() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .avatar(profile != null ? profile.getAvatar() : null)
                .build();
    }
}
