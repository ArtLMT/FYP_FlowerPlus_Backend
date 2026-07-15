package com.lmt.fyp.flowerplus.module.user.application.service;

import com.lmt.fyp.flowerplus.module.user.application.exception.UserNotFoundException;
import com.lmt.fyp.flowerplus.module.user.application.port.in.GetUserUseCase;
import com.lmt.fyp.flowerplus.module.user.application.port.out.LoadUserPort;
import com.lmt.fyp.flowerplus.module.user.domain.model.User;

import java.util.UUID;

/**
 * APPLICATION SERVICE (the use-case implementation) — INSIDE the wall.
 *
 * This is where orchestration lives. Look closely at the imports: there is
 * NOTHING from Spring or JPA here. No @Service, no @Autowired. It receives
 * its out-port through a plain constructor.
 *
 * Because it has no framework annotations, Spring does not pick it up by
 * scanning. Instead we register it explicitly as a @Bean in
 * {@code infrastructure.config.UserBeanConfig}. That keeps the application
 * layer 100% framework-free — the purest form of the dependency rule.
 *
 * (Pragmatic alternative: annotate this with @Service and delete the config
 * bean. That is simpler but lets one Spring import cross the wall. Either is
 * defensible; this project shows the strict version.)
 */
public class UserService implements GetUserUseCase {

    private final LoadUserPort loadUserPort;

    public UserService(LoadUserPort loadUserPort) {
        this.loadUserPort = loadUserPort;
    }

    @Override
    public User getUserById(UUID id) {
        return loadUserPort.loadById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }
}
