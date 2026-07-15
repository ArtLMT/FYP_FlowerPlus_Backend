package com.lmt.fyp.flowerplus.module.user.web.dto;

import com.lmt.fyp.flowerplus.common.AuthProvider;
import com.lmt.fyp.flowerplus.common.UserAccountStatus;
import com.lmt.fyp.flowerplus.common.UserRole;
import com.lmt.fyp.flowerplus.module.user.domain.model.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * RESPONSE DTO — OUTSIDE the wall (web layer).
 *
 * This is a DATA holder (nouns/fields), the shape of the JSON we send back.
 * It is a web concern and must NOT leak into the core — the domain model
 * never depends on it. The controller flattens the domain User (+ its nested
 * profile) into this flat wire shape.
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

    /** Translate the DOMAIN model into the wire shape. */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .provider(user.getProvider())
                .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                .phone(user.getProfile() != null ? user.getProfile().getPhone() : null)
                .avatar(user.getProfile() != null ? user.getProfile().getAvatar() : null)
                .build();
    }
}
