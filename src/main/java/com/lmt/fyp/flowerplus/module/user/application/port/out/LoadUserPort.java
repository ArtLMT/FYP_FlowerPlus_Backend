package com.lmt.fyp.flowerplus.module.user.application.port.out;

import com.lmt.fyp.flowerplus.module.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * OUT-PORT (a "driven" port) — INSIDE the wall.
 *
 * This is the DOOR the service uses to reach the outside world for data.
 * The service CALLS this interface; it does not know or care whether the
 * implementation talks to PostgreSQL, MongoDB, or an in-memory map.
 *
 * Note it speaks purely in DOMAIN terms (returns {@link User}, the domain
 * model) — never JPA entities. The adapter is responsible for translating.
 */
public interface LoadUserPort {
    Optional<User> loadById(UUID id);

    Optional<User> loadByEmail(String email);
}
