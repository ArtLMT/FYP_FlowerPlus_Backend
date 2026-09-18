package com.lmt.fyp.flowerplus.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Supplies {@code created_by} / {@code updated_by} for auditable entities: the
 * signed-in user's id. Rules and what an empty value means: docs/modules/user.md.
 *
 * <p>Only a {@link SecurityUser} principal counts. Anonymous requests and the
 * Google login principal give empty, so those writes record {@code null}.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(SecurityUser.class::isInstance)
                .map(SecurityUser.class::cast)
                .map(SecurityUser::getId);
    }
}
