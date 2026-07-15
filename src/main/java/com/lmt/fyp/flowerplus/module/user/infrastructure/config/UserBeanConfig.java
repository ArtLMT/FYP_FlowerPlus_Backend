package com.lmt.fyp.flowerplus.module.user.infrastructure.config;

import com.lmt.fyp.flowerplus.module.user.application.port.in.GetUserUseCase;
import com.lmt.fyp.flowerplus.module.user.application.port.out.LoadUserPort;
import com.lmt.fyp.flowerplus.module.user.application.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WIRING — OUTSIDE the wall (infrastructure).
 *
 * The application service is framework-free, so Spring cannot discover it by
 * component scanning. Here — in the infrastructure layer, where framework
 * knowledge is allowed — we assemble it: take the LoadUserPort bean (provided
 * by UserPersistenceAdapter) and construct the UserService, exposing it as a
 * GetUserUseCase bean for the controller to inject.
 *
 * This is the seam that keeps all Spring wiring OUT of the core.
 */
@Configuration
public class UserBeanConfig {

    @Bean
    public GetUserUseCase getUserUseCase(LoadUserPort loadUserPort) {
        return new UserService(loadUserPort);
    }
}
