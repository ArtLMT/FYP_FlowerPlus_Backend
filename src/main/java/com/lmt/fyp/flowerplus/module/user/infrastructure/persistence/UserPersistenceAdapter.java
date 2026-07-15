package com.lmt.fyp.flowerplus.module.user.infrastructure.persistence;

import com.lmt.fyp.flowerplus.module.user.application.port.out.LoadUserPort;
import com.lmt.fyp.flowerplus.module.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * PERSISTENCE ADAPTER — OUTSIDE the wall (infrastructure).
 *
 * Implements the out-port {@link LoadUserPort}. The ONLY class that knows both
 * the port AND Spring Data JPA. It loads JPA entities and hands the DOMAIN
 * model back up through the port, so nothing above it depends on JPA.
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort {

    private final UserJpaRepository userJpaRepository;
    private final UserProfileJpaRepository userProfileJpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public Optional<User> loadById(UUID id) {
        return userJpaRepository.findById(id)
                .map(entity -> mapper.toDomain(entity, findProfile(entity)));
    }

    @Override
    public Optional<User> loadByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(entity -> mapper.toDomain(entity, findProfile(entity)));
    }

    private UserProfileJpaEntity findProfile(UserJpaEntity entity) {
        return userProfileJpaRepository.findByUser(entity).orElse(null);
    }
}
