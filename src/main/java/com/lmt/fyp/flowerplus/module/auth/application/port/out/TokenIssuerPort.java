package com.lmt.fyp.flowerplus.module.auth.application.port.out;

import com.lmt.fyp.flowerplus.module.user.infrastructure.persistence.UserJpaEntity;

public interface TokenIssuerPort {
    String issueAccessToken(UserJpaEntity user);
}