package com.lmt.fyp.flowerplus.module.user.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * DOMAIN MODEL — lives INSIDE the wall.
 *
 * A plain value object describing a user's profile. It has NO framework
 * imports (no JPA, no Spring, no Jackson). If you deleted Spring Boot from
 * this project tomorrow, this class would not need to change.
 *
 * Lombok is used only for boilerplate; it is a compile-time tool, not a
 * runtime framework, so it does not couple the domain to any framework.
 */
@Getter
@Builder
public class UserProfile {
    private final String fullName;
    private final String phone;
    private final String avatar;
}
