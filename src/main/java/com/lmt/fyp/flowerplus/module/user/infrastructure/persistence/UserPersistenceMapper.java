package com.lmt.fyp.flowerplus.module.user.infrastructure.persistence;

import com.lmt.fyp.flowerplus.module.user.domain.model.User;
import com.lmt.fyp.flowerplus.module.user.domain.model.UserProfile;
import org.springframework.stereotype.Component;

/**
 * MAPPER — OUTSIDE the wall (infrastructure).
 *
 * Translates JPA persistence entities into the pure DOMAIN model. Now that
 * the entities live in this same package, no fully-qualified names are needed
 * and getUsername() returns the real username (the old getActualUsername()
 * workaround is gone with the UserDetails coupling).
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity, UserProfileJpaEntity profileEntity) {

        UserProfile profile = null;
        if (profileEntity != null) {
            profile = UserProfile.builder()
                    .fullName(profileEntity.getFullName())
                    .phone(profileEntity.getPhone())
                    .avatar(profileEntity.getAvatar())
                    .build();
        }

        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .role(entity.getRole())
                .status(entity.getStatus())
                .provider(entity.getProvider())
                .profile(profile)
                .build();
    }
}
