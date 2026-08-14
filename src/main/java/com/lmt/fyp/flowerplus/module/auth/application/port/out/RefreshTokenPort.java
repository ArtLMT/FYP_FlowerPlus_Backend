package com.lmt.fyp.flowerplus.module.auth.application.port.out;

import com.lmt.fyp.flowerplus.module.auth.infrastructure.persistence.RefreshTokenJpaEntity;
import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;

public interface RefreshTokenPort {
    RefreshTokenJpaEntity create(UserJpaEntity user);
    RefreshTokenJpaEntity verify(String token);
    void revoke(String token);

}
