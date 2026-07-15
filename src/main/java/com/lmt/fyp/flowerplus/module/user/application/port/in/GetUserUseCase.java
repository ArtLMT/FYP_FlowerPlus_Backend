package com.lmt.fyp.flowerplus.module.user.application.port.in;

import com.lmt.fyp.flowerplus.module.user.domain.model.User;

import java.util.UUID;

/**
 * IN-PORT (a "driving" port) — INSIDE the wall.
 *
 * This is the DOOR the web layer knocks on. The controller depends on THIS
 * interface, never on the concrete service. It describes a capability the
 * application offers to the outside world: "you can get a user by id".
 *
 * Contrast with a DTO: this is a verb (an action/method), not a data holder.
 */
public interface GetUserUseCase {
    User getUserById(UUID id);
}
